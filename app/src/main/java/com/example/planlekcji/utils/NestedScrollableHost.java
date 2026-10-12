package com.example.planlekcji.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.widget.ViewPager2;

/**
 * Wrapper layout for handling touch conflicts in nested ViewPager2 setups.
 * Based on Google's NestedScrollableHost sample.
 *
 * Ensures full gesture commitment during horizontal swipes while preserving
 * pull-to-refresh and vertical scrolling.
 */
public class NestedScrollableHost extends FrameLayout {

    private int touchSlop;
    private float initialX;
    private float initialY;
    private boolean isGestureLocked = false;
    private SwipeRefreshLayout lockedSwipeRefresh;

    public NestedScrollableHost(@NonNull Context context) {
        super(context);
        init(context);
    }

    public NestedScrollableHost(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    private ViewPager2 getParentViewPager() {
        for (ViewParent p = getParent(); p != null; p = p.getParent()) {
            if (p instanceof ViewPager2 vp) return vp;
        }
        return null;
    }

    private SwipeRefreshLayout findSwipeRefresh() {
        for (ViewParent p = getParent(); p != null; p = p.getParent()) {
            if (p instanceof SwipeRefreshLayout srl && srl.isEnabled()) return srl;
        }
        return null;
    }

    private void releaseLockedSwipeRefresh() {
        if (lockedSwipeRefresh != null) {
            lockedSwipeRefresh.setEnabled(true);
            lockedSwipeRefresh = null;
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int action = ev.getActionMasked();
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            isGestureLocked = false;
            releaseLockedSwipeRefresh();
        } else if (action == MotionEvent.ACTION_DOWN) {
            isGestureLocked = false;
            releaseLockedSwipeRefresh();
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        releaseLockedSwipeRefresh();
    }

    private View getChild() {
        return getChildCount() > 0 ? getChildAt(0) : null;
    }

    private boolean canChildScroll(int orientation, float delta) {
        int direction = -(int) Math.signum(delta);
        View child = getChild();
        if (child == null || direction == 0) return false;

        if (child instanceof ViewPager2 childPager) {
            int itemCount = childPager.getAdapter() != null ? childPager.getAdapter().getItemCount() : 0;
            int currentItem = childPager.getCurrentItem();

            if (orientation == ViewPager2.ORIENTATION_HORIZONTAL) {
                if (direction > 0 && currentItem >= itemCount - 1) return false;
                if (direction < 0 && currentItem <= 0) return false;
                return true;
            }
        }

        return orientation == ViewPager2.ORIENTATION_HORIZONTAL
                ? child.canScrollHorizontally(direction)
                : child.canScrollVertically(direction);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        handleInterceptTouchEvent(ev);
        return super.onInterceptTouchEvent(ev);
    }

    private void handleInterceptTouchEvent(MotionEvent ev) {
        ViewPager2 parentViewPager = getParentViewPager();
        if (parentViewPager == null) return;

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN -> {
                initialX = ev.getX();
                initialY = ev.getY();
                isGestureLocked = false;
                getParent().requestDisallowInterceptTouchEvent(true);
            }
            case MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isGestureLocked = false;
                getParent().requestDisallowInterceptTouchEvent(false);
                releaseLockedSwipeRefresh();
            }
            case MotionEvent.ACTION_MOVE -> {
                if (isGestureLocked) return;

                float dx = ev.getX() - initialX;
                float dy = ev.getY() - initialY;
                float absDx = Math.abs(dx);
                float absDy = Math.abs(dy);

                if (absDx > touchSlop || absDy > touchSlop) {
                    isGestureLocked = true;
                    boolean isHorizontal = absDx >= absDy;

                    if (parentViewPager.getOrientation() == ViewPager2.ORIENTATION_HORIZONTAL && isHorizontal) {
                        getParent().requestDisallowInterceptTouchEvent(canChildScroll(ViewPager2.ORIENTATION_HORIZONTAL, dx));
                        lockedSwipeRefresh = findSwipeRefresh();
                        if (lockedSwipeRefresh != null) lockedSwipeRefresh.setEnabled(false);
                    } else {
                        getParent().requestDisallowInterceptTouchEvent(false);
                    }
                }
            }
        }
    }
}
