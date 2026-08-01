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
import androidx.viewpager2.widget.ViewPager2;

/**
 * Wrapper layout for handling touch conflicts in nested ViewPager2 setups.
 * Based on Google's NestedScrollableHost sample.
 *
 * @see <a href="https://github.com/android/views-widgets-samples/blob/master/ViewPager2/app/src/main/java/androidx/viewpager2/integration/testapp/NestedScrollableHost.kt">Google sample</a>
 */
public class NestedScrollableHost extends FrameLayout {

    private int touchSlop;
    private float initialX;
    private float initialY;

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
        ViewParent parent = getParent();
        while (parent != null) {
            if (parent instanceof ViewPager2) {
                return (ViewPager2) parent;
            }
            if (parent instanceof View) {
                parent = ((View) parent).getParent();
            } else {
                break;
            }
        }
        return null;
    }

    private View getChild() {
        return getChildCount() > 0 ? getChildAt(0) : null;
    }

    private boolean canChildScroll(int orientation, float delta) {
        int direction = -(int) Math.signum(delta);
        View child = getChild();
        if (child == null) return false;

        // Use logical page position for ViewPager2 children to avoid
        // false positives from canScrollHorizontally() during settle animations.
        if (child instanceof ViewPager2) {
            ViewPager2 childPager = (ViewPager2) child;
            int itemCount = childPager.getAdapter() != null ? childPager.getAdapter().getItemCount() : 0;
            int currentItem = childPager.getCurrentItem();

            if (orientation == ViewPager2.ORIENTATION_HORIZONTAL) {
                if (direction > 0 && currentItem >= itemCount - 1) return false;
                if (direction < 0 && currentItem <= 0) return false;
            }
        }

        if (orientation == ViewPager2.ORIENTATION_HORIZONTAL) {
            return child.canScrollHorizontally(direction);
        } else {
            return child.canScrollVertically(direction);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        handleInterceptTouchEvent(ev);
        return super.onInterceptTouchEvent(ev);
    }

    private void handleInterceptTouchEvent(MotionEvent ev) {
        ViewPager2 parentViewPager = getParentViewPager();
        if (parentViewPager == null) return;

        int orientation = parentViewPager.getOrientation();

        if (!canChildScroll(orientation, -1f) && !canChildScroll(orientation, 1f)) {
            return;
        }

        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            initialX = ev.getX();
            initialY = ev.getY();
            getParent().requestDisallowInterceptTouchEvent(true);
        } else if (ev.getAction() == MotionEvent.ACTION_MOVE) {
            float dx = ev.getX() - initialX;
            float dy = ev.getY() - initialY;
            boolean isVpHorizontal = orientation == ViewPager2.ORIENTATION_HORIZONTAL;

            // ViewPager2 touch-slop is 2x the child's touch-slop
            float scaledDx = Math.abs(dx) * (isVpHorizontal ? 0.5f : 1f);
            float scaledDy = Math.abs(dy) * (isVpHorizontal ? 1f : 0.5f);

            if (scaledDx > touchSlop || scaledDy > touchSlop) {
                if (isVpHorizontal == (scaledDy > scaledDx)) {
                    // Perpendicular gesture - let parent handle it
                    getParent().requestDisallowInterceptTouchEvent(false);
                } else {
                    // Parallel gesture - delegate based on child scroll capability
                    boolean childCanScroll = canChildScroll(orientation, isVpHorizontal ? dx : dy);
                    getParent().requestDisallowInterceptTouchEvent(childCanScroll);
                }
            }
        }
    }
}
