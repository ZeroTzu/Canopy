package com.teamname.canopy.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

public class MovableFloatingActionButton extends ExtendedFloatingActionButton implements View.OnTouchListener {
    private final static float CLICK_DRAG_TOLERANCE = 10; // Often, there will be a slight, unintentional, drag when the user taps the FAB, so we need to account for this.

    private float downRawX, downRawY;
    private float dX, dY;

    public MovableFloatingActionButton(Context context) {
        super(context);
        init();
    }

    public MovableFloatingActionButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MovableFloatingActionButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setOnTouchListener(this);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();

        int action = motionEvent.getAction();
        if (action == MotionEvent.ACTION_DOWN) {

            downRawX = motionEvent.getRawX();
            downRawY = motionEvent.getRawY();
            dX = view.getX() - downRawX;
            dY = view.getY() - downRawY;

            return true; // Consumed

        } else if (action == MotionEvent.ACTION_MOVE) {

            int viewWidth = view.getWidth();
            int viewHeight = view.getHeight();

            View viewParent = (View) view.getParent();
            int parentWidth = viewParent.getWidth();
            int parentHeight = viewParent.getHeight();

            float newX = motionEvent.getRawX() + dX;
            newX = Math.max(layoutParams.leftMargin, newX); // Don't allow the FAB past the left hand side of the parent
            newX = Math.min(parentWidth - viewWidth - layoutParams.rightMargin, newX); // Don't allow the FAB past the right hand side of the parent

            float newY = motionEvent.getRawY() + dY;
            newY = Math.max(layoutParams.topMargin, newY); // Don't allow the FAB past the top of the parent
            newY = Math.min(parentHeight - viewHeight - layoutParams.bottomMargin, newY); // Don't allow the FAB past the bottom of the parent

            view.animate()
                    .x(newX)
                    .y(newY)
                    .setDuration(0)
                    .start();

            return true; // Consumed

        } else if (action == MotionEvent.ACTION_UP) {

            float upRawX = motionEvent.getRawX();
            float upRawY = motionEvent.getRawY();

            float upDX = upRawX - downRawX;
            float upDY = upRawY - downRawY;

            if (Math.abs(upDX) < CLICK_DRAG_TOLERANCE && Math.abs(upDY) < CLICK_DRAG_TOLERANCE) { // A click
                return performClick();
            } else { // A drag
                // Calculate snapping to nearest corner
                View viewParent = (View) view.getParent();
                int parentWidth = viewParent.getWidth();
                int parentHeight = viewParent.getHeight();
                int viewWidth = view.getWidth();
                int viewHeight = view.getHeight();

                // Current FAB position
                float currentX = view.getX();
                float currentY = view.getY();

                // Calculate distances to each corner
                float distanceToTopLeft = (currentX - layoutParams.leftMargin) + (currentY - layoutParams.topMargin);
                float distanceToTopRight = (parentWidth - currentX - viewWidth - layoutParams.rightMargin) + (currentY - layoutParams.topMargin);
                float distanceToBottomLeft = (currentX - layoutParams.leftMargin) + (parentHeight - currentY - viewHeight - layoutParams.bottomMargin);
                float distanceToBottomRight = (parentWidth - currentX - viewWidth - layoutParams.rightMargin) + (parentHeight - currentY - viewHeight - layoutParams.bottomMargin);

                // Determine the nearest corner
                float minDistance = Math.min(Math.min(distanceToTopLeft, distanceToTopRight), Math.min(distanceToBottomLeft, distanceToBottomRight));

                float snapX, snapY;
                if (minDistance == distanceToTopLeft) {
                    snapX = layoutParams.leftMargin;
                    snapY = layoutParams.topMargin;
                } else if (minDistance == distanceToTopRight) {
                    snapX = parentWidth - viewWidth - layoutParams.rightMargin;
                    snapY = layoutParams.topMargin;
                } else if (minDistance == distanceToBottomLeft) {
                    snapX = layoutParams.leftMargin;
                    snapY = parentHeight - viewHeight - layoutParams.bottomMargin;
                } else { // Bottom-right corner
                    snapX = parentWidth - viewWidth - layoutParams.rightMargin;
                    snapY = parentHeight - viewHeight - layoutParams.bottomMargin;
                }

                // Smooth animation to snap position
                view.animate()
                        .x(snapX)
                        .y(snapY)
                        .setDuration(300) // Adjust duration for smoothness
                        .start();

                return true; // Consumed
            }

        } else {
            return super.onTouchEvent(motionEvent);
        }
    }
}
