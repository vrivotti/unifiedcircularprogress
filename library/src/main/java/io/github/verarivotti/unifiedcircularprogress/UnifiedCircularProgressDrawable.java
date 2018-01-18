package io.github.verarivotti.unifiedcircularprogress;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.Keyframe;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

public final class UnifiedCircularProgressDrawable extends Drawable implements Animatable {
    private static final float ANGULAR_EPSILON = 1 / 3600f;

    private static final float BORDER_WIDTH = 4f;
    private static final RectF RECT_BOUNDS = new RectF(-24, -24, 24, 24);
    private static final RectF RECT_PROGRESS = new RectF(-19, -19, 19, 19);

    private final RectF fBounds = new RectF();
    private Paint mPaint = null;

    private int mAlpha = 0xFF;
    private ColorFilter mColorFilter;
    private ColorStateList mTintList;
    private PorterDuff.Mode mTintMode = PorterDuff.Mode.SRC_IN;
    private PorterDuffColorFilter mTintFilter;

    private ValueAnimator mRingPathStart;
    private ValueAnimator mRingPathEnd;
    private float ringStart = 0;
    private float ringEnd = 0;

    private boolean mIndeterminate = true;
    private float mProgress = 0;
    private long mDuration = 1333;

    public UnifiedCircularProgressDrawable() {
        setupIndeterminateAnimators();
        start();
    }

    @Override
    public int getAlpha() {
        return mAlpha;
    }

    @Override
    public void setAlpha(int alpha) {
        if (mAlpha != alpha) {
            mAlpha = alpha;
            invalidateSelf();
        }
    }

    @Override
    public void setTint(@ColorInt int tintColor) {
        setTintList(ColorStateList.valueOf(tintColor));
    }

    @Override
    public void setTintList(@Nullable ColorStateList tint) {
        mTintList = tint;
        if (updateTintFilter()) {
            invalidateSelf();
        }
    }

    @Override
    public void setTintMode(@NonNull PorterDuff.Mode tintMode) {
        mTintMode = tintMode;
        if (updateTintFilter()) {
            invalidateSelf();
        }
    }

    @Override
    public boolean isStateful() {
        return mTintList != null && mTintList.isStateful();
    }

    @Override
    protected boolean onStateChange(int[] state) {
        return updateTintFilter();
    }

    private boolean updateTintFilter() {

        if (mTintList == null || mTintMode == null) {
            boolean hadTintFilter = mTintFilter != null;
            mTintFilter = null;
            return hadTintFilter;
        }

        int tintColor = mTintList.getColorForState(getState(), Color.TRANSPARENT);
        // They made PorterDuffColorFilter.setColor() and setMode() @hide.
        mTintFilter = new PorterDuffColorFilter(tintColor, mTintMode);
        return true;
    }

    @Override
    public ColorFilter getColorFilter() {
        return mColorFilter;
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        mColorFilter = colorFilter;
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    public long getDuration() {
        return mDuration;
    }

    public void setDuration(int duration) {
        this.mDuration = duration;
    }

    public boolean getIndeterminate() {
        return mIndeterminate;
    }

    public void setIndeterminate(boolean indeterminate) {
        if(!indeterminate) setProgress(mProgress);
        else if (!mIndeterminate) {
            mIndeterminate = true;

            reduceAngles();
            if (ringStart < ANGULAR_EPSILON) {
                setupIndeterminateAnimators();
                start();
            }
        }
    }

    public void setProgress(float progress) {
        mProgress = progress;
        mIndeterminate = false;

        setupDeterminateAnimators();
        start();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        if (fBounds.width() == 0 || fBounds.height() == 0) {
            return;
        }

        int saveCount = canvas.save();

        canvas.scale(fBounds.width() / RECT_BOUNDS.width(), fBounds.height() / RECT_BOUNDS.height());
        canvas.translate(RECT_BOUNDS.width() / 2, RECT_BOUNDS.height() / 2);

        if (mPaint == null) {
            mPaint = new Paint();
            mPaint.setAntiAlias(true);
            mPaint.setColor(Color.BLACK);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(BORDER_WIDTH);
            mPaint.setStrokeJoin(Paint.Join.MITER);
            mPaint.setStrokeCap(Paint.Cap.SQUARE);
        }

        mPaint.setAlpha(mAlpha);
        mPaint.setColorFilter(mColorFilter != null ? mColorFilter : mTintFilter);

        float startAngle = 360 * (ringStart) - 90;
        float sweepAngle = 360 * (ringEnd - ringStart);

        canvas.drawArc(RECT_PROGRESS, startAngle, sweepAngle, false, mPaint);
        canvas.restoreToCount(saveCount);

        if (isStarted()) {
            invalidateSelf();
        }
    }

    public void start() {
        if (isStarted()) {
            return;
        }

        mRingPathStart.start();
        mRingPathEnd.start();

        invalidateSelf();
    }

    public void stop() {
        mRingPathStart.end();
        mRingPathEnd.end();
    }

    public boolean isRunning() {
        return mRingPathStart.isRunning();
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);

        fBounds.set(bounds);
    }

    private boolean isStarted() {
        return mRingPathStart.isStarted();
    }

    private void setupDeterminateAnimators() {
        reduceAngles();

        if (ringStart < ANGULAR_EPSILON && ringEnd <= mProgress) {
            setupAnimators(
                PropertyValuesHolder.ofKeyframe("",
                                                Keyframe.ofFloat(0.0f, ringStart),
                                                Keyframe.ofFloat(1.0f, 0.0f)),
                PropertyValuesHolder.ofKeyframe("",
                                                Keyframe.ofFloat(0.0f, ringEnd),
                                                Keyframe.ofFloat(1.0f, mProgress)),
                (long)(mDuration * (mProgress - ringEnd)));
        } else {
            float next = (float)Math.ceil(ringEnd);
            float timeToReset = next - ringStart;
            float timeFraction = timeToReset / (timeToReset + mProgress);

            if(timeFraction > 0.99f) timeFraction = 0.99f;

            setupAnimators(
                PropertyValuesHolder.ofKeyframe("",
                                                Keyframe.ofFloat(0.0f, ringStart),
                                                Keyframe.ofFloat(timeFraction, next),
                                                Keyframe.ofFloat(1.0f, next)),
                PropertyValuesHolder.ofKeyframe("",
                                                Keyframe.ofFloat(0.0f, ringEnd),
                                                Keyframe.ofFloat(timeFraction, next),
                                                Keyframe.ofFloat(1.0f, next + mProgress)),
                (long)(mDuration * (timeToReset + mProgress)));
        }
    }

    private void setupIndeterminateAnimators() {
        reduceAngles();

        if (ringEnd - ringStart < 0.8f) {
            float base = ringStart < ANGULAR_EPSILON ? 0 : ringStart;

            setupAnimators(
                PropertyValuesHolder.ofKeyframe("",
                                                Keyframe.ofFloat(0.0f, ringStart),
                                                Keyframe.ofFloat(0.5f, base + 0.2f),
                                                Keyframe.ofFloat(0.7f, base + 0.8f),
                                                Keyframe.ofFloat(1.0f, base + 1.2f)),
                PropertyValuesHolder.ofKeyframe("",
                                                Keyframe.ofFloat(0.0f, ringEnd),
                                                Keyframe.ofFloat(0.2f, base + 0.65f),
                                                Keyframe.ofFloat(0.5f, base + 1.05f),
                                                Keyframe.ofFloat(1.0f, base + 1.25f)),
                    mDuration);
        } else {
            float next = (float)Math.ceil(ringEnd);
            float timeToReset = next - ringStart;

            setupAnimators(
                PropertyValuesHolder.ofKeyframe("",
                                                Keyframe.ofFloat(0.0f, ringStart),
                                                Keyframe.ofFloat(1.0f, next)),
                PropertyValuesHolder.ofKeyframe("",
                                                Keyframe.ofFloat(0.0f, ringEnd),
                                                Keyframe.ofFloat(1.0f, next + 0.05f)),
                (long)(mDuration * timeToReset));
        }
    }

    private static void cleanUpAnimator(ValueAnimator animator) {
        if (animator != null) {
            animator.removeAllListeners();
            animator.cancel();
        }
    }

    private void setupAnimators(PropertyValuesHolder startValues, PropertyValuesHolder endValues, long duration) {
        cleanUpAnimator(mRingPathStart);
        cleanUpAnimator(mRingPathEnd);

        mRingPathStart = ValueAnimator.ofPropertyValuesHolder(startValues);
        mRingPathStart.setInterpolator(null);
        mRingPathStart.setDuration(duration);
        mRingPathStart.addUpdateListener(anim -> ringStart = (float)anim.getAnimatedValue());
        mRingPathStart.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);

                if (mIndeterminate) {
                    setupIndeterminateAnimators();
                    mRingPathStart.start();
                    mRingPathEnd.start();
                }
            }
        });

        mRingPathEnd = ValueAnimator.ofPropertyValuesHolder(endValues);
        mRingPathEnd.setDuration(duration);
        mRingPathEnd.setInterpolator(null);
        mRingPathEnd.addUpdateListener(anim -> ringEnd = (float)anim.getAnimatedValue());
    }

    private void reduceAngles() {
        if (ringEnd < ringStart) {
            ringEnd = ringStart;
        }

        if (ringEnd > ringStart + 1) {
            ringEnd = ringStart + 1;
        }

        if (ringStart >= 1 || ringStart < 0) {
            double f = Math.floor(ringStart);
            ringStart = (float)(ringStart - f);
            ringEnd = (float)(ringEnd - f);
        }
    }
}