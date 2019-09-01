package green_green_avk.anotherterm.ui;

import android.content.Context;
import android.graphics.PointF;
import android.support.v4.math.MathUtils;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.Scroller;

public abstract class ScrollableView extends GestureView {

    public final PointF scrollPosition = new PointF(0, 0);
    protected final PointF scrollScale = new PointF(16, 16);
    protected final Scroller mScroller;

    public boolean scrollDisabled = false;

    public ScrollableView(final Context context) {
        super(context);
        mScroller = new Scroller(getContext());
    }

    public ScrollableView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        mScroller = new Scroller(getContext());
    }

    public ScrollableView(final Context context, final AttributeSet attrs,
                          final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mScroller = new Scroller(getContext());
    }

    public ScrollableView(final Context context, final AttributeSet attrs,
                          final int defStyleAttr, final int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mScroller = new Scroller(getContext());
    }

    public void setScrollScale(final float x, final float y) {
        mScroller.forceFinished(true);
        scrollScale.x = x;
        scrollScale.y = y;
    }

    private float toFloatX(final int v) {
        return (float) v / scrollScale.x;
    }

    private float toFloatY(final int v) {
        return (float) v / scrollScale.y;
    }

    private int toIntX(final float v) {
        return (int) (v * scrollScale.x);
    }

    private int toIntY(final float v) {
        return (int) (v * scrollScale.y);
    }

    protected float getLeftScrollLimit() {
        return 0;
    }

    protected float getTopScrollLimit() {
        return 0;
    }

    protected float getRightScrollLimit() {
        return 0;
    }

    protected float getBottomScrollLimit() {
        return 0;
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mScroller.computeScrollOffset()) {
            scrollPosition.x = toFloatX(mScroller.getCurrX());
            scrollPosition.y = toFloatY(mScroller.getCurrY());
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override
    public boolean onDown(final MotionEvent e) {
        mScroller.forceFinished(true);
        return true;
    }

    @Override
    public boolean onScroll(final MotionEvent e1, final MotionEvent e2,
                            final float distanceX, final float distanceY) {
        if (scrollDisabled) return true;
        final int x1 = toIntX(scrollPosition.x);
        final int y1 = toIntY(scrollPosition.y);
        final int x2 = MathUtils.clamp((int) (distanceX) + x1,
                toIntX(getLeftScrollLimit()), toIntX(getRightScrollLimit()));
        final int y2 = MathUtils.clamp((int) (distanceY) + y1,
                toIntY(getTopScrollLimit()), toIntY(getBottomScrollLimit()));
//        mScroller.startScroll(x1, y1, x2 - x1, y2 - y1);
        scrollPosition.x = toFloatX(x2);
        scrollPosition.y = toFloatY(y2);
        ViewCompat.postInvalidateOnAnimation(this);
        return true;
    }

    @Override
    public boolean onFling(final MotionEvent e1, final MotionEvent e2,
                           final float velocityX, final float velocityY) {
        mScroller.forceFinished(true);
        if (scrollDisabled) return true;
        mScroller.fling(toIntX(scrollPosition.x), toIntY(scrollPosition.y),
                -(int) velocityX, -(int) velocityY,
                toIntX(getLeftScrollLimit()),
                toIntX(getRightScrollLimit()),
                toIntY(getTopScrollLimit()),
                toIntY(getBottomScrollLimit())
        );
        ViewCompat.postInvalidateOnAnimation(this);
        return true;
    }
}
