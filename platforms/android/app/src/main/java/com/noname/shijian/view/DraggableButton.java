package com.noname.shijian.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import androidx.appcompat.widget.AppCompatButton;

import com.noname.shijian.R;

public class DraggableButton extends AppCompatButton {

    private int lastTouchX;
    private int lastTouchY;
    private ViewGroup.MarginLayoutParams layoutParams;
    private int dragThreshold; // 拖动阈值
    private float downX, downY;
    private boolean isDragging;

    public DraggableButton(Context context) {
        super(context);
        init();
    }

    public DraggableButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DraggableButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        layoutParams = (ViewGroup.MarginLayoutParams) getLayoutParams();
        this.setText("调试");
        this.setBackgroundResource(R.drawable.chrome_icon);
    }

    @Override
    public void setLayoutParams(android.view.ViewGroup.LayoutParams params) {
        super.setLayoutParams(params);
        layoutParams = (ViewGroup.MarginLayoutParams) params;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = (int) event.getX();
                lastTouchY = (int) event.getY();
                downX = event.getX();
                downY = event.getY();
                isDragging = false;
                return super.onTouchEvent(event);
            case MotionEvent.ACTION_MOVE:
            if (!isDragging) { // 只有当没有开始拖动时才检查阈值
                float dX = Math.abs(event.getX() - downX);
                float dY = Math.abs(event.getY() - downY);
                if (dX >= dragThreshold || dY >= dragThreshold) {
                    isDragging = true; // 开始拖动
                    lastTouchX = (int) event.getX();
                    lastTouchY = (int) event.getY();
                }
            } else {
                // 如果已经在拖动，则继续处理拖动逻辑
                float dX = event.getRawX() - lastTouchX;
                float dY = event.getRawY() - lastTouchY;

                // 更新位置
                // 考虑初始Margin，计算按钮的新位置
                int newX = (int) (layoutParams.leftMargin + dX);
                int newY = (int) (layoutParams.topMargin + dY);

                // 限制拖动范围，让按钮在屏幕边缘最多只露出一半，同时考虑到初始Margin
                newX = Math.min(Math.max(newX, -getMeasuredWidth() / 2 + layoutParams.leftMargin), getWidth() - getMeasuredWidth() / 2 + layoutParams.leftMargin);
                newY = Math.min(Math.max(newY, -getMeasuredHeight() / 2 + layoutParams.topMargin), getHeight() - getMeasuredHeight() / 2 + layoutParams.topMargin);

                layoutParams.leftMargin = newX;
                layoutParams.topMargin = newY;

                requestLayout();

                // 更新lastTouchX和lastTouchY
                lastTouchX = (int) event.getRawX();
                lastTouchY = (int) event.getRawY();
            }
            break;
            case MotionEvent.ACTION_UP:
                isDragging = false;
                // 判断移动幅度，如果很小则视为点击
                float deltaX = Math.abs(event.getX() - downX);
                float deltaY = Math.abs(event.getY() - downY);
                if (deltaX < dragThreshold && deltaY < dragThreshold) {
                    return super.onTouchEvent(event); // 允许父类或其他监听器处理点击
                } else {
                    // 移动幅度较大，不执行任何点击处理
                    return true; // 消费ACTION_UP事件，避免触发点击
                }
            default:
                return super.onTouchEvent(event);
        }
        return true;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // 在按钮被添加到窗口时设置拖动阈值
        dragThreshold = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }
}
