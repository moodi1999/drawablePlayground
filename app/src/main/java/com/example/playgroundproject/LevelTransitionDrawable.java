package com.example.playgroundproject;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Outline;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ScaleDrawable;
import android.os.Build;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Interpolator;

import androidx.annotation.RequiresApi;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class LevelTransitionDrawable extends Drawable implements Drawable.Callback {

    public static final int PADDING_MODE_NEST = 0;

    /**
     * Padding mode used to stack each layer directly atop the previous layer.
     *
     * @see #setPaddingMode(int)
     */
    public static final int PADDING_MODE_STACK = 1;

    LayerState mLayerState;

    private int mOpacityOverride = PixelFormat.UNKNOWN;
    private int[] mPaddingL;
    private int[] mPaddingT;
    private int[] mPaddingR;
    private int[] mPaddingB;

    private final Rect mTmpRect = new Rect();
    private Rect mHotspotBounds;
    private boolean mMutated;

    /**
     * Create a new layer drawable with the list of specified layers.
     *
     * @param layers A list of drawables to use as layers in this new drawable.
     */
    public LevelTransitionDrawable(Drawable[] layers) {
        this(layers, null);
    }

    /**
     * Create a new layer drawable with the specified list of layers and the
     * specified constant state.
     *
     * @param layers The list of layers to add to this drawable.
     * @param state The constant drawable state.
     */
    LevelTransitionDrawable(Drawable[] layers, LayerState state) {
        this(state, null);
        final int length = layers.length;
        final ChildDrawable[] r = new ChildDrawable[length];
        for (int i = 0; i < length; i++) {
            r[i] = new ChildDrawable();
            r[i].mDrawable = new ScaleDrawable(layers[i], Gravity.CENTER, 1f, 1f);
            layers[i].setCallback(this);
            mLayerState.mChildrenChangingConfigurations |= layers[i].getChangingConfigurations();
        }
        mLayerState.mNum = length;
        mLayerState.mChildren = r;

        ensurePadding();
    }

    LevelTransitionDrawable() {
        this((LayerState) null, null);
    }

    LevelTransitionDrawable(LayerState state, Resources res) {
        mLayerState = createConstantState(state, res);
        if (mLayerState.mNum > 0) {
            ensurePadding();
        }
    }

    LayerState createConstantState(LayerState state, Resources res) {
        return new LayerState(state, this, res);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs, Theme theme)
            throws XmlPullParserException, IOException {
        super.inflate(r, parser, attrs, theme);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            inflateLayers(r, parser, attrs, theme);
        }

        ensurePadding();
        onStateChange(getState());
    }

    /**
     * Inflates child layers using the specified parser.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void inflateLayers(Resources r, XmlPullParser parser, AttributeSet attrs, Theme theme)
            throws XmlPullParserException, IOException {
        final LayerState state = mLayerState;

        final int innerDepth = parser.getDepth() + 1;
        int type;
        int depth;
        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                && ((depth = parser.getDepth()) >= innerDepth || type != XmlPullParser.END_TAG)) {
            if (type != XmlPullParser.START_TAG) {
                continue;
            }

            if (depth > innerDepth || !parser.getName().equals("item")) {
                continue;
            }

            final ChildDrawable layer = new ChildDrawable();

            if (layer.mDrawable == null) {
                while ((type = parser.next()) == XmlPullParser.TEXT) {
                }
                if (type != XmlPullParser.START_TAG) {
                    throw new XmlPullParserException(parser.getPositionDescription()
                            + ": <item> tag requires a 'drawable' attribute or "
                            + "child tag defining a drawable");
                }
                layer.mDrawable = Drawable.createFromXmlInner(r, parser, attrs, theme);
            }

            if (layer.mDrawable != null) {
                state.mChildrenChangingConfigurations |=
                        layer.mDrawable.getChangingConfigurations();
                layer.mDrawable.setCallback(this);
            }

            addLayer(layer);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public boolean canApplyTheme() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return (mLayerState != null && mLayerState.canApplyTheme()) || super.canApplyTheme();
        }
        return false;
    }

    void addLayer(ChildDrawable layer) {
        final LayerState st = mLayerState;
        final int N = st.mChildren != null ? st.mChildren.length : 0;
        final int i = st.mNum;
        if (i >= N) {
            final ChildDrawable[] nu = new ChildDrawable[N + 10];
            if (i > 0) {
                System.arraycopy(st.mChildren, 0, nu, 0, i);
            }

            st.mChildren = nu;
        }

        st.mChildren[i] = layer;
        st.mNum++;
        st.invalidateCache();
    }

    /**
     * Add a new layer to this drawable. The new layer is identified by an id.
     *
     * @param layer The drawable to add as a layer.
     * @param themeAttrs Theme attributes extracted from the layer.
     * @param id The id of the new layer.
     * @param left The left padding of the new layer.
     * @param top The top padding of the new layer.
     * @param right The right padding of the new layer.
     * @param bottom The bottom padding of the new layer.
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    ChildDrawable addLayer(Drawable layer, int[] themeAttrs, int id, int left, int top, int right,
                           int bottom) {
        final ChildDrawable childDrawable = new ChildDrawable();
        childDrawable.mId = id;
        childDrawable.mThemeAttrs = themeAttrs;
        childDrawable.mDrawable = layer;
        childDrawable.mDrawable.setAutoMirrored(isAutoMirrored());
        childDrawable.mInsetL = left;
        childDrawable.mInsetT = top;
        childDrawable.mInsetR = right;
        childDrawable.mInsetB = bottom;

        addLayer(childDrawable);

        mLayerState.mChildrenChangingConfigurations |= layer.getChangingConfigurations();
        layer.setCallback(this);

        return childDrawable;
    }

    /**
     * Looks for a layer with the given ID and returns its {@link Drawable}.
     * <p>
     * If multiple layers are found for the given ID, returns the
     * {@link Drawable} for the matching layer at the highest index.
     *
     * @param id The layer ID to search for.
     * @return The {@link Drawable} for the highest-indexed layer that has the
     *         given ID, or null if not found.
     */
    public Drawable findDrawableByLayerId(int id) {
        final ChildDrawable[] layers = mLayerState.mChildren;
        for (int i = mLayerState.mNum - 1; i >= 0; i--) {
            if (layers[i].mId == id) {
                return layers[i].mDrawable;
            }
        }

        return null;
    }

    /**
     * Sets the ID of a layer.
     *
     * @param index The index of the layer which will received the ID.
     * @param id The ID to assign to the layer.
     */
    public void setId(int index, int id) {
        mLayerState.mChildren[index].mId = id;
    }

    /**
     * Returns the number of layers contained within this.
     * @return The number of layers.
     */
    public int getNumberOfLayers() {
        return mLayerState.mNum;
    }

    /**
     * Returns the drawable at the specified layer index.
     *
     * @param index The layer index of the drawable to retrieve.
     *
     * @return The {@link android.graphics.drawable.Drawable} at the specified layer index.
     */
    public Drawable getDrawable(int index) {
        return mLayerState.mChildren[index].mDrawable;
    }

    /**
     * Returns the id of the specified layer.
     *
     * @param index The index of the layer.
     *
     * @return The id of the layer or {@link android.view.View#NO_ID} if the layer has no id.
     */
    public int getId(int index) {
        return mLayerState.mChildren[index].mId;
    }

    /**
     * Sets (or replaces) the {@link Drawable} for the layer with the given id.
     *
     * @param id The layer ID to search for.
     * @param drawable The replacement {@link Drawable}.
     * @return Whether the {@link Drawable} was replaced (could return false if
     *         the id was not found).
     */
    public boolean setDrawableByLayerId(int id, Drawable drawable) {
        final ChildDrawable[] layers = mLayerState.mChildren;
        final int N = mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            final ChildDrawable childDrawable = layers[i];
            if (childDrawable.mId == id) {
                if (childDrawable.mDrawable != null) {
                    if (drawable != null) {
                        final Rect bounds = childDrawable.mDrawable.getBounds();
                        drawable.setBounds(bounds);
                    }

                    childDrawable.mDrawable.setCallback(null);
                }

                if (drawable != null) {
                    drawable.setCallback(this);
                }

                childDrawable.mDrawable = drawable;
                mLayerState.invalidateCache();
                return true;
            }
        }

        return false;
    }

    /**
     * Specifies the insets in pixels for the drawable at the specified index.
     *
     * @param index the index of the drawable to adjust
     * @param l number of pixels to add to the left bound
     * @param t number of pixels to add to the top bound
     * @param r number of pixels to subtract from the right bound
     * @param b number of pixels to subtract from the bottom bound
     */
    public void setLayerInset(int index, int l, int t, int r, int b) {
        final ChildDrawable childDrawable = mLayerState.mChildren[index];
        childDrawable.mInsetL = l;
        childDrawable.mInsetT = t;
        childDrawable.mInsetR = r;
        childDrawable.mInsetB = b;
    }

    /**
     * Specifies how layer padding should affect the bounds of subsequent
     * layers. The default value is {@link #PADDING_MODE_NEST}.
     *
     * @param mode padding mode, one of:
     *            <ul>
     *            <li>{@link #PADDING_MODE_NEST} to nest each layer inside the
     *            padding of the previous layer
     *            <li>{@link #PADDING_MODE_STACK} to stack each layer directly
     *            atop the previous layer
     *            </ul>
     *
     * @see #getPaddingMode()
     * @attr ref android.R.styleable#LayerDrawable_paddingMode
     */
    public void setPaddingMode(int mode) {
        if (mLayerState.mPaddingMode != mode) {
            mLayerState.mPaddingMode = mode;
        }
    }

    /**
     * @return the current padding mode
     *
     * @see #setPaddingMode(int)
     * @attr ref android.R.styleable#LayerDrawable_paddingMode
     */
    public int getPaddingMode() {
        return mLayerState.mPaddingMode;
    }

    @Override
    public void invalidateDrawable(Drawable who) {
        invalidateSelf();
    }

    @Override
    public void scheduleDrawable(Drawable who, Runnable what, long when) {
        scheduleSelf(what, when);
    }

    @Override
    public void unscheduleDrawable(Drawable who, Runnable what) {
        unscheduleSelf(what);
    }

    @Override
    public void draw(Canvas canvas) {
        final ChildDrawable[] array = mLayerState.mChildren;
        final int N = mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            array[i].mDrawable.draw(canvas);
        }
    }

    @Override
    public int getChangingConfigurations() {
        return super.getChangingConfigurations()
                | mLayerState.mChangingConfigurations
                | mLayerState.mChildrenChangingConfigurations;
    }

    @Override
    public boolean getPadding(Rect padding) {
        if (mLayerState.mPaddingMode == PADDING_MODE_NEST) {
            computeNestedPadding(padding);
        } else {
            computeStackedPadding(padding);
        }

        return padding.left != 0 || padding.top != 0 || padding.right != 0 || padding.bottom != 0;
    }

    private void computeNestedPadding(Rect padding) {
        padding.left = 0;
        padding.top = 0;
        padding.right = 0;
        padding.bottom = 0;

        // Add all the padding.
        final ChildDrawable[] array = mLayerState.mChildren;
        final int N = mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            refreshChildPadding(i, array[i]);

            padding.left += mPaddingL[i];
            padding.top += mPaddingT[i];
            padding.right += mPaddingR[i];
            padding.bottom += mPaddingB[i];
        }
    }

    private void computeStackedPadding(Rect padding) {
        padding.left = 0;
        padding.top = 0;
        padding.right = 0;
        padding.bottom = 0;

        // Take the max padding.
        final ChildDrawable[] array = mLayerState.mChildren;
        final int N = mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            refreshChildPadding(i, array[i]);

            padding.left = Math.max(padding.left, mPaddingL[i]);
            padding.top = Math.max(padding.top, mPaddingT[i]);
            padding.right = Math.max(padding.right, mPaddingR[i]);
            padding.bottom = Math.max(padding.bottom, mPaddingB[i]);
        }
    }

    /**
     * Populates <code>outline</code> with the first available (non-empty) layer outline.
     *
     * @param outline Outline in which to place the first available layer outline
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void getOutline(Outline outline) {
        final LayerState state = mLayerState;
        final ChildDrawable[] children = state.mChildren;
        final int N = state.mNum;
        for (int i = 0; i < N; i++) {
            children[i].mDrawable.getOutline(outline);
            if (!outline.isEmpty()) {
                return;
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void setHotspot(float x, float y) {
        final ChildDrawable[] array = mLayerState.mChildren;
        final int N = mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            array[i].mDrawable.setHotspot(x, y);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void setHotspotBounds(int left, int top, int right, int bottom) {
        final ChildDrawable[] array = mLayerState.mChildren;
        final int N = mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            array[i].mDrawable.setHotspotBounds(left, top, right, bottom);
        }

        if (mHotspotBounds == null) {
            mHotspotBounds = new Rect(left, top, right, bottom);
        } else {
            mHotspotBounds.set(left, top, right, bottom);
        }
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        final boolean changed = super.setVisible(visible, restart);
        final ChildDrawable[] array = mLayerState.mChildren;
        final int N = mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            array[i].mDrawable.setVisible(visible, restart);
        }

        return changed;
    }

    @Override
    public void setDither(boolean dither) {
        final ChildDrawable[] array = mLayerState.mChildren;
        final int N = mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            array[i].mDrawable.setDither(dither);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        final ChildDrawable[] array = mLayerState.mChildren;
        final int N = mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            array[i].mDrawable.setAlpha(alpha);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public int getAlpha() {
        final ChildDrawable[] array = mLayerState.mChildren;
        if (mLayerState.mNum > 0) {
            // All layers should have the same alpha set on them - just return
            // the first one
            return array[0].mDrawable.getAlpha();
        } else {
            return super.getAlpha();
        }
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        final ChildDrawable[] array = mLayerState.mChildren;
        final int N = mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            array[i].mDrawable.setColorFilter(cf);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void setTintList(ColorStateList tint) {
        final ChildDrawable[] array = mLayerState.mChildren;
        final int N = mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            array[i].mDrawable.setTintList(tint);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void setTintMode(Mode tintMode) {
        final ChildDrawable[] array = mLayerState.mChildren;
        final int N = mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            array[i].mDrawable.setTintMode(tintMode);
        }
    }

    /**
     * Sets the opacity of this drawable directly, instead of collecting the
     * states from the layers
     *
     * @param opacity The opacity to use, or {@link PixelFormat#UNKNOWN
     *            PixelFormat.UNKNOWN} for the default behavior
     * @see PixelFormat#UNKNOWN
     * @see PixelFormat#TRANSLUCENT
     * @see PixelFormat#TRANSPARENT
     * @see PixelFormat#OPAQUE
     */
    public void setOpacity(int opacity) {
        mOpacityOverride = opacity;
    }

    @Override
    public int getOpacity() {
        if (mOpacityOverride != PixelFormat.UNKNOWN) {
            return mOpacityOverride;
        }
        return mLayerState.getOpacity();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void setAutoMirrored(boolean mirrored) {
        mLayerState.mAutoMirrored = mirrored;

        final ChildDrawable[] array = mLayerState.mChildren;
        final int N = mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            array[i].mDrawable.setAutoMirrored(mirrored);
        }
    }

    @Override
    public boolean isAutoMirrored() {
        return mLayerState.mAutoMirrored;
    }

    @Override
    public boolean isStateful() {
        return mLayerState.isStateful();
    }

    @Override
    protected boolean onStateChange(int[] state) {
        boolean paddingChanged = false;
        boolean changed = false;

        final ChildDrawable[] array = mLayerState.mChildren;
        final int N = mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            final ChildDrawable r = array[i];
            if (r.mDrawable.isStateful() && r.mDrawable.setState(state)) {
                changed = true;
            }

            if (refreshChildPadding(i, r)) {
                paddingChanged = true;
            }
        }

        if (paddingChanged) {
            onBoundsChange(getBounds());
        }

        return changed;
    }

    @Override
    protected boolean onLevelChange(int level) {
        boolean paddingChanged = false;
        boolean changed = false;

        final ChildDrawable[] array = mLayerState.mChildren;
        final int N = mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            final ChildDrawable r = array[i];
            if (r.mDrawable.setLevel(level)) {
                changed = true;
            }

            if (refreshChildPadding(i, r)) {
                paddingChanged = true;
            }
        }

        if (paddingChanged) {
            onBoundsChange(getBounds());
        }

        return changed;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        int padL = 0;
        int padT = 0;
        int padR = 0;
        int padB = 0;

        final boolean nest = mLayerState.mPaddingMode == PADDING_MODE_NEST;
        final ChildDrawable[] array = mLayerState.mChildren;
        final int N = mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            final ChildDrawable r = array[i];
            r.mDrawable.setBounds(bounds.left + r.mInsetL + padL, bounds.top + r.mInsetT + padT,
                    bounds.right - r.mInsetR - padR, bounds.bottom - r.mInsetB - padB);

            if (nest) {
                padL += mPaddingL[i];
                padR += mPaddingR[i];
                padT += mPaddingT[i];
                padB += mPaddingB[i];
            }
        }
    }

    @Override
    public int getIntrinsicWidth() {
        int width = -1;
        int padL = 0;
        int padR = 0;

        final boolean nest = mLayerState.mPaddingMode == PADDING_MODE_NEST;
        final ChildDrawable[] array = mLayerState.mChildren;
        final int N = mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            final ChildDrawable r = array[i];
            final int w = r.mDrawable.getIntrinsicWidth() + r.mInsetL + r.mInsetR + padL + padR;
            if (w > width) {
                width = w;
            }

            if (nest) {
                padL += mPaddingL[i];
                padR += mPaddingR[i];
            }
        }

        return width;
    }

    @Override
    public int getIntrinsicHeight() {
        int height = -1;
        int padT = 0;
        int padB = 0;

        final boolean nest = mLayerState.mPaddingMode == PADDING_MODE_NEST;
        final ChildDrawable[] array = mLayerState.mChildren;
        final int N = mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            final ChildDrawable r = array[i];
            int h = r.mDrawable.getIntrinsicHeight() + r.mInsetT + r.mInsetB + padT + padB;
            if (h > height) {
                height = h;
            }

            if (nest) {
                padT += mPaddingT[i];
                padB += mPaddingB[i];
            }
        }

        return height;
    }

    /**
     * Refreshes the cached padding values for the specified child.
     *
     * @return true if the child's padding has changed
     */
    private boolean refreshChildPadding(int i, ChildDrawable r) {
        final Rect rect = mTmpRect;
        r.mDrawable.getPadding(rect);
        if (rect.left != mPaddingL[i] || rect.top != mPaddingT[i] ||
                rect.right != mPaddingR[i] || rect.bottom != mPaddingB[i]) {
            mPaddingL[i] = rect.left;
            mPaddingT[i] = rect.top;
            mPaddingR[i] = rect.right;
            mPaddingB[i] = rect.bottom;
            return true;
        }
        return false;
    }

    /**
     * Ensures the child padding caches are large enough.
     */
    void ensurePadding() {
        final int N = mLayerState.mNum;
        if (mPaddingL != null && mPaddingL.length >= N) {
            return;
        }

        mPaddingL = new int[N];
        mPaddingT = new int[N];
        mPaddingR = new int[N];
        mPaddingB = new int[N];
    }

    @Override
    public ConstantState getConstantState() {
        if (mLayerState.canConstantState()) {
            mLayerState.mChangingConfigurations = getChangingConfigurations();
            return mLayerState;
        }
        return null;
    }

    @Override
    public Drawable mutate() {
        if (!mMutated && super.mutate() == this) {
            mLayerState = createConstantState(mLayerState, null);
            final ChildDrawable[] array = mLayerState.mChildren;
            final int N = mLayerState.mNum;
            for (int i = 0; i < N; i++) {
                array[i].mDrawable.mutate();
            }
            mMutated = true;
        }
        return this;
    }

    static class ChildDrawable {
        public Drawable mDrawable;
        public int[] mThemeAttrs;
        public int mInsetL, mInsetT, mInsetR, mInsetB;
        public int mId = View.NO_ID;

        ChildDrawable() {
            // Default empty constructor.
        }

        ChildDrawable(ChildDrawable orig, LevelTransitionDrawable owner, Resources res) {
            if (res != null) {
                mDrawable = orig.mDrawable.getConstantState().newDrawable(res);
            } else {
                mDrawable = orig.mDrawable.getConstantState().newDrawable();
            }
            mDrawable.setCallback(owner);
            mDrawable.setBounds(orig.mDrawable.getBounds());
            mDrawable.setLevel(orig.mDrawable.getLevel());
            mThemeAttrs = orig.mThemeAttrs;
            mInsetL = orig.mInsetL;
            mInsetT = orig.mInsetT;
            mInsetR = orig.mInsetR;
            mInsetB = orig.mInsetB;
            mId = orig.mId;
        }
    }

    public static class LayerState extends ConstantState {
        int mNum;
        ChildDrawable[] mChildren;
        int[] mThemeAttrs;

        int mChangingConfigurations;
        int mChildrenChangingConfigurations;

        private boolean mHaveOpacity;
        private int mOpacity;

        private boolean mHaveIsStateful;
        private boolean mIsStateful;

        private boolean mAutoMirrored = false;

        private int mPaddingMode = PADDING_MODE_NEST;

        LayerState(LayerState orig, LevelTransitionDrawable owner, Resources res) {
            if (orig != null) {
                final ChildDrawable[] origChildDrawable = orig.mChildren;
                final int N = orig.mNum;

                mNum = N;
                mChildren = new ChildDrawable[N];

                mChangingConfigurations = orig.mChangingConfigurations;
                mChildrenChangingConfigurations = orig.mChildrenChangingConfigurations;

                for (int i = 0; i < N; i++) {
                    final ChildDrawable or = origChildDrawable[i];
                    mChildren[i] = new ChildDrawable(or, owner, res);
                }

                mHaveOpacity = orig.mHaveOpacity;
                mOpacity = orig.mOpacity;
                mHaveIsStateful = orig.mHaveIsStateful;
                mIsStateful = orig.mIsStateful;
                mAutoMirrored = orig.mAutoMirrored;
                mPaddingMode = orig.mPaddingMode;
                mThemeAttrs = orig.mThemeAttrs;
            } else {
                mNum = 0;
                mChildren = null;
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public boolean canApplyTheme() {
            if (mThemeAttrs != null || super.canApplyTheme()) {
                return true;
            }

            final ChildDrawable[] array = mChildren;
            final int N = mNum;
            for (int i = 0; i < N; i++) {
                final ChildDrawable layer = array[i];
                if (layer.mThemeAttrs != null || layer.mDrawable.canApplyTheme()) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public Drawable newDrawable() {
            return new LevelTransitionDrawable(this, null);
        }

        @Override
        public Drawable newDrawable(Resources res) {
            return new LevelTransitionDrawable(this, res);
        }

        @Override
        public int getChangingConfigurations() {
            return mChangingConfigurations;
        }

        public final int getOpacity() {
            if (mHaveOpacity) {
                return mOpacity;
            }

            final ChildDrawable[] array = mChildren;
            final int N = mNum;
            int op = N > 0 ? array[0].mDrawable.getOpacity() : PixelFormat.TRANSPARENT;
            for (int i = 1; i < N; i++) {
                op = Drawable.resolveOpacity(op, array[i].mDrawable.getOpacity());
            }

            mOpacity = op;
            mHaveOpacity = true;
            return op;
        }

        public final boolean isStateful() {
            if (mHaveIsStateful) {
                return mIsStateful;
            }

            final ChildDrawable[] array = mChildren;
            final int N = mNum;
            boolean isStateful = false;
            for (int i = 0; i < N; i++) {
                if (array[i].mDrawable.isStateful()) {
                    isStateful = true;
                    break;
                }
            }

            mIsStateful = isStateful;
            mHaveIsStateful = true;
            return isStateful;
        }

        public final boolean canConstantState() {
            final ChildDrawable[] array = mChildren;
            final int N = mNum;
            for (int i = 0; i < N; i++) {
                if (array[i].mDrawable.getConstantState() == null) {
                    return false;
                }
            }

            // Don't cache the result, this method is not called very often.
            return true;
        }

        public void invalidateCache() {
            mHaveOpacity = false;
            mHaveIsStateful = false;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static ScaleLevelTransitionDrawable make(Interpolator interpolator, Context context, int... drawableRedIds) {
        Drawable []layers = new Drawable[drawableRedIds.length];
        for(int i = 0; i < drawableRedIds.length; i++) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                layers[i] = context.getDrawable(drawableRedIds[i]);
            }
        }
        return make(interpolator, layers);
    }

    public static ScaleLevelTransitionDrawable make(Interpolator interpolator, Drawable... layers) {
        ScaleLevelTransitionDrawable drawable = new ScaleLevelTransitionDrawable(layers);
        drawable.setInterpolator(interpolator);
        return drawable;
    }

    public static class ScaleLevelTransitionDrawable extends LevelTransitionDrawable implements Drawable.Callback {

        private static final int MAX_LEVEL = 10000;


        /**
         * A transition is about to start.
         */
        private static final int TRANSITION_STARTING = 0;

        /**
         * The transition has started and the animation is in progress
         */
        private static final int TRANSITION_RUNNING = 1;

        /**
         * No transition will be applied
         */
        private static final int TRANSITION_NONE = 2;

        /**
         * The current state of the transition. One of {@link #TRANSITION_STARTING},
         * {@link #TRANSITION_RUNNING} and {@link #TRANSITION_NONE}
         */
        private int mTransitionState = TRANSITION_NONE;

        private boolean mReverse;
        private long mStartTimeMillis;
        private int mFrom;
        private int mTo;
        private int mDuration;
        private int mOriginalDuration;
        private int mAlpha = 0;
        private boolean mCrossFade;

        private Interpolator interpolator;

        /**
         * Create a new transition drawable with the specified list of layers. At least
         * 2 layers are required for this drawable to work properly.
         */
        public ScaleLevelTransitionDrawable(Drawable[] layers) {
            this(new TransitionState(null, null, null), layers);
        }

        /**
         * Create a new transition drawable with no layer. To work correctly, at least 2
         * layers must be added to this drawable.
         *
         * @see #ScaleLevelTransitionDrawable(Drawable[])
         */
        ScaleLevelTransitionDrawable() {
            this(new TransitionState(null, null, null), (Resources) null);
        }

        private ScaleLevelTransitionDrawable(TransitionState state, Resources res) {
            super(state, res);
        }

        private ScaleLevelTransitionDrawable(TransitionState state, Drawable[] layers) {
            super(layers, state);
        }

        @Override
        LayerState createConstantState(LayerState state, Resources res) {
            return new TransitionState((TransitionState) state, this, res);
        }

        /**
         * Begin the second layer on top of the first layer.
         *
         * @param durationMillis The length of the transition in milliseconds
         */
        public void startTransition(int durationMillis) {
            mFrom = 0;
            mTo = 255;
            mAlpha = 0;
            mDuration = mOriginalDuration = durationMillis;
            mReverse = false;
            mTransitionState = TRANSITION_STARTING;
            invalidateSelf();
        }

        /**
         * Show only the first layer.
         */
        public void resetTransition() {
            mAlpha = 0;
            mTransitionState = TRANSITION_NONE;
            invalidateSelf();
        }

        /**
         * Reverses the transition, picking up where the transition currently is.
         * If the transition is not currently running, this will start the transition
         * with the specified duration. If the transition is already running, the last
         * known duration will be used.
         *
         * @param duration The duration to use if no transition is running.
         */
        public void reverseTransition(int duration) {
            final long time = SystemClock.uptimeMillis();
            // Animation is over
            if (time - mStartTimeMillis > mDuration) {
                if (mTo == 0) {
                    mFrom = 0;
                    mTo = 255;
                    mAlpha = 0;
                    mReverse = false;
                } else {
                    mFrom = 255;
                    mTo = 0;
                    mAlpha = 255;
                    mReverse = true;
                }
                mDuration = mOriginalDuration = duration;
                mTransitionState = TRANSITION_STARTING;
                invalidateSelf();
                return;
            }

            mReverse = !mReverse;
            mFrom = mAlpha;
            mTo = mReverse ? 0 : 255;
            mDuration = (int) (mReverse ? time - mStartTimeMillis :
                    mOriginalDuration - (time - mStartTimeMillis));
            mTransitionState = TRANSITION_STARTING;
        }

        public void setInterpolator(Interpolator interpolator) {
            this.interpolator = interpolator;
        }

        @Override
        public void draw(Canvas canvas) {
            boolean done = true;

            switch (mTransitionState) {
                case TRANSITION_STARTING:
                    mStartTimeMillis = SystemClock.uptimeMillis();
                    done = false;
                    mTransitionState = TRANSITION_RUNNING;
                    break;

                case TRANSITION_RUNNING:
                    if (mStartTimeMillis >= 0) {
                        float normalized = (float)
                                (SystemClock.uptimeMillis() - mStartTimeMillis) / mDuration;
                        done = normalized >= 1.0f;
                        normalized = Math.min(normalized, 1.0f);
                        mAlpha = (int) (mFrom  + (mTo - mFrom) * normalized);
                    }
                    break;
            }

            final int alpha = mAlpha;
            final boolean crossFade = mCrossFade;
            final ChildDrawable[] array = mLayerState.mChildren;

            if (done) {
                // the setAlpha() calls below trigger invalidation and redraw. If we're done, just draw
                // the appropriate drawable[s] and return
                if (!crossFade || alpha == 0) {
                    array[0].mDrawable.setLevel(MAX_LEVEL);
                    array[0].mDrawable.draw(canvas);
                }
                if (alpha == 0xFF) {
                    array[1].mDrawable.setLevel(MAX_LEVEL);
                    array[1].mDrawable.draw(canvas);
                }
                return;
            }

            int level = getLevelFromAlpha(alpha);

            Drawable d;
            d = array[0].mDrawable;
            if (crossFade) {
                d.setLevel(MAX_LEVEL - level);
            }
            d.draw(canvas);
            if (crossFade) {
                d.setLevel(MAX_LEVEL);
            }

            if (level > 0) {
                d = array[1].mDrawable;
                d.setLevel(level);
                d.draw(canvas);
                d.setLevel(MAX_LEVEL);
            }

            if (!done) {
                invalidateSelf();
            }
        }

        private int getLevelFromAlpha(int alpha) {
            float levelFloat = ((float)alpha) / 255;
            if(interpolator!=null) {
                levelFloat = interpolator.getInterpolation(levelFloat);
            }
            return  (int) (levelFloat * MAX_LEVEL);
        }

        /**
         * Enables or disables the cross fade of the drawables. When cross fade
         * is disabled, the first drawable is always drawn opaque. With cross
         * fade enabled, the first drawable is drawn with the opposite alpha of
         * the second drawable. Cross fade is disabled by default.
         *
         * @param enabled True to enable cross fading, false otherwise.
         */
        public void setCrossFadeEnabled(boolean enabled) {
            mCrossFade = enabled;
        }

        /**
         * Indicates whether the cross fade is enabled for this transition.
         *
         * @return True if cross fading is enabled, false otherwise.
         */
        public boolean isCrossFadeEnabled() {
            return mCrossFade;
        }

        static class TransitionState extends LayerState {
            TransitionState(TransitionState orig, ScaleLevelTransitionDrawable owner, Resources res) {
                super(orig, owner, res);
            }

            @Override
            public Drawable newDrawable() {
                return new ScaleLevelTransitionDrawable(this, (Resources) null);
            }

            @Override
            public Drawable newDrawable(Resources res) {
                return new ScaleLevelTransitionDrawable(this, res);
            }

            @Override
            public int getChangingConfigurations() {
                return mChangingConfigurations;
            }
        }
    }
}