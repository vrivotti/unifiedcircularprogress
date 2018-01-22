package io.github.verarivotti.unifiedcircularprogress;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.util.Pools;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewDebug;
import android.widget.ProgressBar;
import android.widget.RemoteViews.RemoteView;

import java.util.ArrayList;

/**
 * <p>
 * A user interface element that indicates the progress of an operation.
 * Progress bar supports two modes to represent progress: determinate, and indeterminate.
 * Transitions between modes are smooth and continuous.
 * </p>
 * <h3>Indeterminate Progress</h3>
 * <p>
 * Use indeterminate mode for the progress bar when you do not know how long an
 * operation will take.
 * Indeterminate mode is the default for progress bar and shows a cyclic animation without a
 * specific amount of progress indicated.
 * </p>
 * <h3>Determinate Progress</h3>
 * <p>
 * Use determinate mode for the progress bar when you want to show that a specific quantity of
 * progress has occurred.
 * Determinate mode shows an animation to specific amount of progress.
 * <p>
 * You can update the percentage of progress displayed by using the
 * {@link #setProgress(int)} method, or by calling
 * {@link #incrementProgressBy(int)} to increase the current progress completed
 * by a specified amount.
 * By default, the progress bar is full when the progress value reaches 100.
 * You can adjust this default by setting the
 * {@link R.styleable#UnifiedCircularProgressBar_android_max android:max} attribute.
 * </p>
 *
 * <p>
 * See {@link R.styleable#UnifiedCircularProgressBar Attributes}
 * </p>
 */
@RemoteView
public class UnifiedCircularProgressBar extends View {

    int mMinWidth;
    int mMaxWidth;
    int mMinHeight;
    int mMaxHeight;
    private int mProgress;
    private int mMin;
    private boolean mMinInitialized;
    private int mMax;
    private boolean mMaxInitialized;
	private boolean mIndeterminate;
    private UnifiedCircularProgressDrawable mIndeterminateDrawable;
    private ProgressTintInfo mProgressTintInfo;

    private boolean mNoInvalidate;
    private RefreshProgressRunnable mRefreshProgressRunnable;
    private long mUiThreadId;
    private boolean mShouldStartAnimationDrawable;
    private boolean mAttached;
    private boolean mRefreshIsPosted;

	boolean mMirrorForRtl = false;
    private boolean mAggregatedIsVisible;

    private final ArrayList<RefreshData> mRefreshData = new ArrayList<RefreshData>();

    /**
     * Create a new progress bar with range 0...100, initial progress of 0 and in indeterminate mode.
     * @param context the application environment
     */
    public UnifiedCircularProgressBar(Context context) {
        super(context, null);

        init(context, null, 0, 0);
    }

    public UnifiedCircularProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);

        init(context, attrs, 0, 0);
    }

    public UnifiedCircularProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context, attrs, defStyleAttr, 0);
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public UnifiedCircularProgressBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        init(context, attrs, defStyleAttr, defStyleRes);
    }

    public void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        mUiThreadId = Thread.currentThread().getId();
        initProgressBar();

        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.UnifiedCircularProgressBar, defStyleAttr, defStyleRes);

        mNoInvalidate = true;
        mMinWidth = a.getDimensionPixelSize(R.styleable.UnifiedCircularProgressBar_android_minWidth, mMinWidth);
        mMaxWidth = a.getDimensionPixelSize(R.styleable.UnifiedCircularProgressBar_android_maxWidth, mMaxWidth);
        mMinHeight = a.getDimensionPixelSize(R.styleable.UnifiedCircularProgressBar_android_minHeight, mMinHeight);
        mMaxHeight = a.getDimensionPixelSize(R.styleable.UnifiedCircularProgressBar_android_maxHeight, mMaxHeight);
        setMin(a.getInt(R.styleable.UnifiedCircularProgressBar_android_min, mMin));
        setMax(a.getInt(R.styleable.UnifiedCircularProgressBar_android_max, mMax));
        setProgress(a.getInt(R.styleable.UnifiedCircularProgressBar_android_progress, mProgress));
		setIndeterminateDrawable(new UnifiedCircularProgressDrawable());
        mNoInvalidate = false;
		setIndeterminate(a.getBoolean(
				R.styleable.UnifiedCircularProgressBar_android_indeterminate, mIndeterminate));
        mMirrorForRtl = a.getBoolean(R.styleable.UnifiedCircularProgressBar_android_mirrorForRtl, mMirrorForRtl);

        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(R.attr.colorControlActivated, typedValue, true);
        ColorStateList defaultTint = ColorStateList.valueOf(typedValue.data);

        mProgressTintInfo = new ProgressTintInfo();
        mProgressTintInfo.mHasIndeterminateTint = true;
        mProgressTintInfo.mIndeterminateTintList = defaultTint;
        mProgressTintInfo.mHasIndeterminateTint = true;

        if (a.hasValue(R.styleable.UnifiedCircularProgressBar_android_indeterminateTintMode)) {
            mProgressTintInfo.mIndeterminateTintMode = parseTintMode(a.getInt(
                    R.styleable.UnifiedCircularProgressBar_android_indeterminateTintMode, -1), null);
            mProgressTintInfo.mHasIndeterminateTintMode = true;
        }
        if (a.hasValue(R.styleable.UnifiedCircularProgressBar_android_indeterminateTint)) {
            mProgressTintInfo.mIndeterminateTintList = a.getColorStateList(
                    R.styleable.UnifiedCircularProgressBar_android_indeterminateTint);
            mProgressTintInfo.mHasIndeterminateTint = true;
        }
        a.recycle();
        applyIndeterminateTint();
	}
    /**
     * <p>
     * Initialize the progress bar's default values:
     * </p>
     * <ul>
     * <li>progress = 0</li>
     * <li>min = 0</li>
     * <li>max = 100</li>
     * <li>indeterminate = false</li>
     * </ul>
     */
    private void initProgressBar() {
        mMin = 0;
        mMax = 100;
        mProgress = 0;
		mIndeterminate = false;
        mMinWidth = 24;
        mMaxWidth = 48;
        mMinHeight = 24;
        mMaxHeight = 48;
    }
    /**
     * <p>Indicate whether this progress bar is in indeterminate mode.</p>
     *
     * @return true if the progress bar is in indeterminate mode
     */
    @ViewDebug.ExportedProperty(category = "progress")
    public synchronized boolean isIndeterminate() {
        return mIndeterminate;
    }

    /**
     * <p>Change the indeterminate mode for this progress bar. In indeterminate
     * mode, the progress is ignored and the progress bar shows an infinite
     * animation instead.</p>
     *
     * @param indeterminate true to enable the indeterminate mode
     */
    public synchronized void setIndeterminate(boolean indeterminate) {
        mIndeterminate = indeterminate;
        mIndeterminateDrawable.setIndeterminate(indeterminate);

        startAnimation();
    }
    @TargetApi(Build.VERSION_CODES.M)
    private void setIndeterminateDrawable(UnifiedCircularProgressDrawable d) {
        if (mIndeterminateDrawable != d) {
            mIndeterminateDrawable = d;
            if (d != null) {
                d.setCallback(this);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    d.setLayoutDirection(getLayoutDirection());
                }
                if (d.isStateful()) {
                    d.setState(getDrawableState());
                }
                applyIndeterminateTint();
            }
            postInvalidate();
        }
    }
    /**
     * Applies a tint to the drawable. Does not modify the
     * current tint mode, which is {@link PorterDuff.Mode#SRC_IN} by default.
     * <p>
     *
     * @param tint the tint to apply, may be {@code null} to clear tint
     *
     * @see #getIndeterminateTintList()
     * @see Drawable#setTintList(ColorStateList)
     */
    public void setIndeterminateTintList(@Nullable ColorStateList tint) {
        if (mProgressTintInfo == null) {
            mProgressTintInfo = new ProgressTintInfo();
        }
        mProgressTintInfo.mIndeterminateTintList = tint;
        mProgressTintInfo.mHasIndeterminateTint = true;
        applyIndeterminateTint();
    }
    /**
     * @return the tint applied to the indeterminate drawable
     * @see #setIndeterminateTintList(ColorStateList)
     */
    @Nullable
    public ColorStateList getIndeterminateTintList() {
        return mProgressTintInfo != null ? mProgressTintInfo.mIndeterminateTintList : null;
    }
    /**
     * Specifies the blending mode used to apply the tint specified by
     * {@link #setIndeterminateTintList(ColorStateList)} to the indeterminate
     * drawable. The default mode is {@link PorterDuff.Mode#SRC_IN}.
     *
     * @param tintMode the blending mode used to apply the tint, may be
     *                 {@code null} to clear tint
     * @see #setIndeterminateTintList(ColorStateList)
     * @see Drawable#setTintMode(PorterDuff.Mode)
     */
    public void setIndeterminateTintMode(@Nullable PorterDuff.Mode tintMode) {
        if (mProgressTintInfo == null) {
            mProgressTintInfo = new ProgressTintInfo();
        }
        mProgressTintInfo.mIndeterminateTintMode = tintMode;
        mProgressTintInfo.mHasIndeterminateTintMode = true;
        applyIndeterminateTint();
    }
    /**
     * Returns the blending mode used to apply the tint to the indeterminate
     * drawable, if specified.
     *
     * @return the blending mode used to apply the tint to the indeterminate
     *         drawable
     * @see #setIndeterminateTintMode(PorterDuff.Mode)
     */
    @Nullable
    public PorterDuff.Mode getIndeterminateTintMode() {
        return mProgressTintInfo != null ? mProgressTintInfo.mIndeterminateTintMode : null;
    }
    private void applyIndeterminateTint() {
        if (mIndeterminateDrawable != null && mProgressTintInfo != null) {
            final ProgressTintInfo tintInfo = mProgressTintInfo;
            if (tintInfo.mHasIndeterminateTint || tintInfo.mHasIndeterminateTintMode) {
                mIndeterminateDrawable = (UnifiedCircularProgressDrawable)mIndeterminateDrawable.mutate();
                if (tintInfo.mHasIndeterminateTint) {
                    mIndeterminateDrawable.setTintList(tintInfo.mIndeterminateTintList);
                }
                if (tintInfo.mHasIndeterminateTintMode) {
                    mIndeterminateDrawable.setTintMode(tintInfo.mIndeterminateTintMode);
                }
                // The drawable (or one of its children) may not have been
                // stateful before applying the tint, so let's try again.
                if (mIndeterminateDrawable.isStateful()) {
                    mIndeterminateDrawable.setState(getDrawableState());
                }
            }
        }
    }
    @Override
    protected boolean verifyDrawable(@NonNull Drawable who) {
        return who == mIndeterminateDrawable
                || super.verifyDrawable(who);
    }
    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        if (mIndeterminateDrawable != null) mIndeterminateDrawable.jumpToCurrentState();
    }
    @RequiresApi(Build.VERSION_CODES.M)
    public void onResolveDrawables(int layoutDirection) {
        final Drawable d = mIndeterminateDrawable;
        if (d != null) {
            d.setLayoutDirection(layoutDirection);
        }
    }
    @Override
    public void postInvalidate() {
        if (!mNoInvalidate) {
            super.postInvalidate();
        }
    }
    private class RefreshProgressRunnable implements Runnable {
        public void run() {
            synchronized (UnifiedCircularProgressBar.this) {
                final int count = mRefreshData.size();
                for (int i = 0; i < count; i++) {
                    final RefreshData rd = mRefreshData.get(i);
                    doRefreshProgress(rd.progress);
                    rd.recycle();
                }
                mRefreshData.clear();
                mRefreshIsPosted = false;
            }
        }
    }

    private static class RefreshData {
        private static final int POOL_MAX = 24;
        private static final Pools.SynchronizedPool<RefreshData> sPool =
                new Pools.SynchronizedPool<RefreshData>(POOL_MAX);

        public int progress;
        public static RefreshData obtain(int progress) {
            RefreshData rd = sPool.acquire();
            if (rd == null) {
                rd = new RefreshData();
            }
            rd.progress = progress;
            return rd;
        }

        public void recycle() {
            sPool.release(this);
        }
    }

    private synchronized void doRefreshProgress(int progress) {
        int range = mMax - mMin;
        final float scale = range > 0 ? (progress - mMin) / (float) range : 0;
        mIndeterminateDrawable.setProgress(scale);
    }

    private synchronized void refreshProgress(int progress) {
        if (mUiThreadId == Thread.currentThread().getId()) {
            doRefreshProgress(progress);
        } else {
            if (mRefreshProgressRunnable == null) {
                mRefreshProgressRunnable = new RefreshProgressRunnable();
            }

            final RefreshData rd = RefreshData.obtain(progress);
            mRefreshData.add(rd);
            if (mAttached && !mRefreshIsPosted) {
                post(mRefreshProgressRunnable);
                mRefreshIsPosted = true;
            }
        }
    }
    /**
     * Sets the current progress to the specified value.
     * <p>
     * This method will animate the visual position to the target value.
     *
     * @param progress the new progress, between {@link #getMin()} and {@link #getMax()}
     *
     * @see #setIndeterminate(boolean)
     * @see #isIndeterminate()
     * @see #getProgress()
     * @see #incrementProgressBy(int)
     */
    public synchronized void setProgress(int progress) {
        progress = constrain(progress, mMin, mMax);

        if (progress == mProgress && !mIndeterminate) {
            // No change from current.
            return;
        }
        mProgress = progress;
        refreshProgress(mProgress);
    }

    private static int constrain(int amount, int low, int high) {
        return amount < low ? low : (amount > high ? high : amount);
    }

    /**
     * <p>Get the progress bar's current level of progress. Return 0 when the
     * progress bar is in indeterminate mode.</p>
     *
     * @return the current progress, between {@link #getMin()} and {@link #getMax()}
     *
     * @see #setIndeterminate(boolean)
     * @see #isIndeterminate()
     * @see #setProgress(int)
     * @see #setMax(int)
     * @see #getMax()
     * @see #setMin(int)
     * @see #getMin()
     */
    @ViewDebug.ExportedProperty(category = "progress")
    public synchronized int getProgress() {
        return mIndeterminate ? 0 : mProgress;
    }

    /**
     * <p>Return the lower limit of this progress bar's range.</p>
     *
     * @return a positive integer
     *
     * @see #setMin(int)
     * @see #getProgress()
     */
    @ViewDebug.ExportedProperty(category = "progress")
    public synchronized int getMin() {
        return mMin;
    }

    /**
     * <p>Return the upper limit of this progress bar's range.</p>
     *
     * @return a positive integer
     *
     * @see #setMax(int)
     * @see #getProgress()
     */
    @ViewDebug.ExportedProperty(category = "progress")
    public synchronized int getMax() {
        return mMax;
    }

    /**
     * <p>Set the lower range of the progress bar to <tt>min</tt>.</p>
     *
     * @param min the lower range of this progress bar
     *
     * @see #getMin()
     * @see #setProgress(int)
     */
    public synchronized void setMin(int min) {
        if (mMaxInitialized) {
            if (min > mMax) {
                min = mMax;
            }
        }
        mMinInitialized = true;
        if (mMaxInitialized && min != mMin) {
            mMin = min;
            postInvalidate();
            if (mProgress < min) {
                mProgress = min;
            }
            refreshProgress(mProgress);
        } else {
            mMin = min;
        }
    }

    /**
     * <p>Set the upper range of the progress bar <tt>max</tt>.</p>
     *
     * @param max the upper range of this progress bar
     *
     * @see #getMax()
     * @see #setProgress(int)
     */
    public synchronized void setMax(int max) {
        if (mMinInitialized) {
            if (max < mMin) {
                max = mMin;
            }
        }
        mMaxInitialized = true;
        if (mMinInitialized && max != mMax) {
            mMax = max;
            postInvalidate();
            if (mProgress > max) {
                mProgress = max;
            }
            refreshProgress(mProgress);
        } else {
            mMax = max;
        }
    }
    /**
     * <p>Increase the progress bar's progress by the specified amount.</p>
     *
     * @param diff the amount by which the progress must be increased
     *
     * @see #setProgress(int)
     */
    public synchronized final void incrementProgressBy(int diff) {
        setProgress(mProgress + diff);
    }
    /**
     * <p>Start the indeterminate progress animation.</p>
     */
    void startAnimation() {
        if (getVisibility() != VISIBLE || getWindowVisibility() != VISIBLE) {
            return;
        }

        mShouldStartAnimationDrawable = true;
        postInvalidate();
    }
    /**
     * <p>Stop the indeterminate progress animation.</p>
     */
    void stopAnimation() {
        mIndeterminateDrawable.stop();
		mShouldStartAnimationDrawable = false;
        postInvalidate();
    }
    @Override
    public void onVisibilityAggregated(boolean isVisible) {
        super.onVisibilityAggregated(isVisible);
        if (isVisible != mAggregatedIsVisible) {
            mAggregatedIsVisible = isVisible;

            // let's be nice with the UI thread
            if (isVisible) {
                startAnimation();
            } else {
                stopAnimation();
            }
            mIndeterminateDrawable.setVisible(isVisible, false);
        }
    }
    @Override
    public void invalidateDrawable(@NonNull Drawable dr) {
        if (verifyDrawable(dr)) {
            final Rect dirty = dr.getBounds();
            final int scrollX = getScrollX() + getPaddingLeft();
            final int scrollY = getScrollY() + getPaddingTop();

            invalidate(dirty.left + scrollX, dirty.top + scrollY,
                    dirty.right + scrollX, dirty.bottom + scrollY);
        } else {
            super.invalidateDrawable(dr);
        }
    }
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        updateDrawableBounds(w, h);
    }
    private void updateDrawableBounds(int w, int h) {
        // onDraw will translate the canvas so we draw starting at 0,0.
        // Subtract out padding for the purposes of the calculations below.
        w -= getPaddingRight() + getPaddingLeft();
        h -= getPaddingTop() + getPaddingBottom();

        int right = w;
        int bottom = h;
        int top = 0;
        int left = 0;
		if (mIndeterminateDrawable != null) {
            // Aspect ratio logic does not apply to AnimationDrawables
                // Maintain aspect ratio. Certain kinds of animated drawables
                // get very confused otherwise.
                final int intrinsicWidth = mIndeterminateDrawable.getIntrinsicWidth();
                final int intrinsicHeight = mIndeterminateDrawable.getIntrinsicHeight();
                final float intrinsicAspect = (float) intrinsicWidth / intrinsicHeight;
                final float boundAspect = (float) w / h;
                if (intrinsicAspect != boundAspect) {
                    if (boundAspect > intrinsicAspect) {
                        // New width is larger. Make it smaller to match height.
                        final int width = (int) (h * intrinsicAspect);
                        left = (w - width) / 2;
                        right = left + width;
                    } else {
                        // New height is larger. Make it smaller to match width.
                        final int height = (int) (w * (1 / intrinsicAspect));
                        top = (h - height) / 2;
                        bottom = top + height;
                    }
                }
            if (isLayoutRtl() && mMirrorForRtl) {
                int tempLeft = left;
                left = w - right;
                right = w - tempLeft;
            }
            mIndeterminateDrawable.setBounds(left, top, right, bottom);
        }
	}
    @Override
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawTrack(canvas);
    }
    /**
     * Draws the progress bar track.
     */
    void drawTrack(Canvas canvas) {
        final UnifiedCircularProgressDrawable d = mIndeterminateDrawable;
        if (d != null) {
            // Translate canvas so a indeterminate circular progress bar with padding
            // rotates properly in its animation
            final int saveCount = canvas.save();
			if (isLayoutRtl() && mMirrorForRtl) {
                canvas.translate(getWidth() - getPaddingRight(), getPaddingTop());
                canvas.scale(-1.0f, 1.0f);
            } else {
				canvas.translate(getPaddingLeft(), getPaddingTop());
			}
            d.draw(canvas);
            canvas.restoreToCount(saveCount);
            if (mShouldStartAnimationDrawable) {
                d.start();
                mShouldStartAnimationDrawable = false;
            }
        }
    }
    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int dw = 0;
        int dh = 0;

        final Drawable d = mIndeterminateDrawable;
        if (d != null) {
            dw = Math.max(mMinWidth, Math.min(mMaxWidth, d.getIntrinsicWidth()));
            dh = Math.max(mMinHeight, Math.min(mMaxHeight, d.getIntrinsicHeight()));
        }
        updateDrawableState();
        dw += getPaddingLeft() + getPaddingRight();
        dh += getPaddingTop() + getPaddingBottom();
        final int measuredWidth = resolveSizeAndState(dw, widthMeasureSpec, 0);
        final int measuredHeight = resolveSizeAndState(dh, heightMeasureSpec, 0);
        setMeasuredDimension(measuredWidth, measuredHeight);
    }
    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        updateDrawableState();
    }
    private void updateDrawableState() {
        final int[] state = getDrawableState();
        boolean changed = false;
        final Drawable indeterminateDrawable = mIndeterminateDrawable;
        if (indeterminateDrawable != null && indeterminateDrawable.isStateful()) {
            changed |= indeterminateDrawable.setState(state);
        }
        if (changed) {
            invalidate();
        }
    }
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	@Override
    public void drawableHotspotChanged(float x, float y) {
        super.drawableHotspotChanged(x, y);
        if (mIndeterminateDrawable != null) {
            mIndeterminateDrawable.setHotspot(x, y);
        }
    }
    static class SavedState extends BaseSavedState {
        int progress;
        /**
         * Constructor called from {@link UnifiedCircularProgressBar#onSaveInstanceState()}
         */
        SavedState(Parcelable superState) {
            super(superState);
        }
        /**
         * Constructor called from {@link #CREATOR}
         */
        private SavedState(Parcel in) {
            super(in);
            progress = in.readInt();
        }
        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(progress);
        }
        public static final Parcelable.Creator<SavedState> CREATOR
                = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
    @Override
    public Parcelable onSaveInstanceState() {
        // Force our ancestor class to save its state
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.progress = mProgress;
        return ss;
    }
    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        setProgress(ss.progress);
    }
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startAnimation();
        if (mRefreshData != null) {
            synchronized (this) {
                final int count = mRefreshData.size();
                for (int i = 0; i < count; i++) {
                    final RefreshData rd = mRefreshData.get(i);
                    doRefreshProgress(rd.progress);
                    rd.recycle();
                }
                mRefreshData.clear();
            }
        }
        mAttached = true;
    }
    @Override
    protected void onDetachedFromWindow() {
        stopAnimation();
        if (mRefreshProgressRunnable != null) {
            removeCallbacks(mRefreshProgressRunnable);
            mRefreshIsPosted = false;
        }
        // This should come after stopAnimation(), otherwise an invalidate message remains in the
        // queue, which can prevent the entire view hierarchy from being GC'ed during a rotation
        super.onDetachedFromWindow();
        mAttached = false;
    }

    /**
     * Returns whether the progress bar is animating or not. This is essentially the same
     * as whether the progress bar is visible.
     *
     * @return true if the progress bar is animating, false otherwise.
     */
    public boolean isAnimating() {
        return getWindowVisibility() == VISIBLE && isShown();
    }

    private static class ProgressTintInfo {
        ColorStateList mIndeterminateTintList;
        PorterDuff.Mode mIndeterminateTintMode;
        boolean mHasIndeterminateTint;
        boolean mHasIndeterminateTintMode;
    }

    /**
     * Returns true if view's layout direction is right-to-left.
     */
    private boolean isLayoutRtl() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
        } else {
            // All layouts are LTR before JB MR1.
            return false;
        }
    }

    /**
     * Parses a {@link android.graphics.PorterDuff.Mode} from a tintMode
     * attribute's enum value.
     *
     */
    private static PorterDuff.Mode parseTintMode(int value, PorterDuff.Mode defaultMode) {
        switch (value) {
            case 3: return PorterDuff.Mode.SRC_OVER;
            case 5: return PorterDuff.Mode.SRC_IN;
            case 9: return PorterDuff.Mode.SRC_ATOP;
            case 14: return PorterDuff.Mode.MULTIPLY;
            case 15: return PorterDuff.Mode.SCREEN;
            case 16: return PorterDuff.Mode.ADD;
            default: return defaultMode;
        }
    }
}
