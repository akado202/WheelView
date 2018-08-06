package com.akado.wheelview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;


/**
 * Created by James Do on 2018. 7. 24..
 */

public class WheelView extends View {

    private static final String TAG = WheelView.class.getSimpleName();

    private static final float VELOCITY_FLING_MAX = 5000f;

    private static final float VELOCITY_FLING_INTERVAL = 20f;

    private static final float VELOCITY_FLING_RATIO = 0.01f;

    private static final int VELOCITY_FLING_DURATION = 20;

    private static final float VELOCITY_INTERPOLATOR_FACTOR = 5f;

    private static final int CLICK_INTERVAL = 120;

    private static final int ANIMATION_DURATION = 350;

    public interface Adapter {
        int getItemsCount();

        String getItem(int index);
    }

    public interface OnItemSelectListener {
        void onItemSelected(int position);
    }

    private Adapter adapter;
    private OnItemSelectListener onItemSelectListener;

    private ObjectAnimator objectAnimator;

    private GestureDetector gestureDetector;

    private String visibles[];
    private int itemHeight;
    private int itemCenterTextHeight;
    private int itemOutTextHeight;

    private int measuredWidth;
    private int measuredHeight;

    private int drawCenterContentStart = 0;
    private int drawOutContentStart = 0;

    private float firstLineY;
    private float secondLineY;

    private long startTime = 0;
    private float previousY = 0;
    private float totalScrollY;

    private int initPosition;
    private int preCurrentIndex;

    private Paint paintOuterText;
    private Paint paintCenterText;
    private Paint paintIndicator;

    public WheelView(Context context) {
        super(context);
        init(context, null);
    }

    public WheelView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public WheelView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public WheelView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(@NonNull Context context, @Nullable AttributeSet attrs) {
        itemHeight = getResources().getDimensionPixelSize(R.dimen.default_wheelview_item_height);
        int textSize = getResources().getDimensionPixelSize(R.dimen.default_wheelview_textsize);
        int textColorOut = ContextCompat.getColor(getContext(), R.color.wheelview_wheelview_textcolor_out);
        int textColorCenter = ContextCompat.getColor(getContext(), R.color.wheelview_wheelview_textcolor_center);
        int dividerColor = ContextCompat.getColor(getContext(), R.color.wheelview_wheelview_textcolor_divider);
        int dividerWidth = getResources().getDimensionPixelSize(R.dimen.default_wheelview_dividerWidth);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.WheelView, 0, 0);
            itemHeight = a.getDimensionPixelSize(R.styleable.WheelView_wheelview_itemHeight, itemHeight);
            textSize = a.getDimensionPixelSize(R.styleable.WheelView_wheelview_textSize, textSize);
            textColorOut = a.getColor(R.styleable.WheelView_wheelview_textColorOut, textColorOut);
            textColorCenter = a.getColor(R.styleable.WheelView_wheelview_textColorCenter, textColorCenter);
            dividerColor = a.getColor(R.styleable.WheelView_wheelview_dividerColor, dividerColor);
            dividerWidth = a.getDimensionPixelSize(R.styleable.WheelView_wheelview_dividerWidth, dividerWidth);
            a.recycle();
        }

        paintOuterText = new Paint();
        paintOuterText.setColor(textColorOut);
        paintOuterText.setAntiAlias(true);
        paintOuterText.setTypeface(Typeface.MONOSPACE);
        paintOuterText.setTextSize(textSize);

        paintCenterText = new Paint();
        paintCenterText.setColor(textColorCenter);
        paintCenterText.setAntiAlias(true);
        paintCenterText.setTextScaleX(1.1f);
        paintCenterText.setTypeface(Typeface.MONOSPACE);
        paintCenterText.setTextSize(textSize);

        paintIndicator = new Paint();
        paintIndicator.setColor(dividerColor);
        paintIndicator.setStrokeWidth(dividerWidth);
        paintIndicator.setAntiAlias(true);

        Rect rect = new Rect();
        paintOuterText.getTextBounds("\u661F\u671F", 0, 2, rect);
        itemOutTextHeight = rect.height();
        paintCenterText.getTextBounds("\u661F\u671F", 0, 2, rect);
        itemCenterTextHeight = rect.height();

        gestureDetector = new GestureDetector(context, new SimpleOnGestureListener() {
            @Override
            public final boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                flingBy(velocityY);
                return true;
            }
        });
        gestureDetector.setIsLongpressEnabled(false);

        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        Log.d(TAG, "++ onLayout: ");
        super.onLayout(changed, left, top, right, bottom);

        if (changed) {
            measuredWidth = right - left;
            measuredHeight = bottom - top;

//            Log.v(TAG, "\t>> measuredWidth : " + measuredWidth);
//            Log.v(TAG, "\t>> measuredHeight : " + measuredHeight);

            int itemsVisible = (int) (measuredHeight / itemHeight);
            itemsVisible += itemsVisible % 2 == 0 ? 1 : 2;
//            Log.v(TAG, "\t>> itemsVisible : " + itemsVisible);

            visibles = new String[itemsVisible];

            firstLineY = (measuredHeight - itemHeight) / 2f;
            secondLineY = (measuredHeight + itemHeight) / 2f;

//            Log.v(TAG, "\t>> firstLineY : " + firstLineY);
//            Log.v(TAG, "\t>> secondLineY : " + secondLineY);

            if (initPosition == -1) {
                initPosition = 0;
            }
            preCurrentIndex = initPosition;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
//        Log.d(TAG, "++ onDraw: ");

        canvas.drawLine(0f, firstLineY, measuredWidth, firstLineY, paintIndicator);
        canvas.drawLine(0f, secondLineY, measuredWidth, secondLineY, paintIndicator);

        if (getAdapter() == null) {
            return;
        }

        initPosition = Math.min(Math.max(0, initPosition), adapter.getItemsCount() - 1);
        preCurrentIndex = initPosition + ((int) (totalScrollY / itemHeight) % adapter.getItemsCount());
        if (preCurrentIndex < 0) {
            preCurrentIndex = 0;
        }
        if (preCurrentIndex > adapter.getItemsCount() - 1) {
            preCurrentIndex = adapter.getItemsCount() - 1;
        }

        int itemsVisible = visibles.length;
        int counter = 0;
        while (counter < itemsVisible) {
            int index = preCurrentIndex - (itemsVisible / 2 - counter);
            if (index < 0) {
                visibles[counter] = "";
            } else if (index > adapter.getItemsCount() - 1) {
                visibles[counter] = "";
            } else {
                visibles[counter] = adapter.getItem(index);
            }

            counter++;
        }

        float itemDiff = ((visibles.length * itemHeight) - measuredHeight) / 2f;
        float itemHeightOffset = (totalScrollY % itemHeight) + itemDiff;

//        Log.v(TAG, "\t>> initPosition : " + initPosition);
//        Log.v(TAG, "\t>> visibles.length * itemHeight : " + visibles.length * itemHeight);
//        Log.v(TAG, "\t>> measuredHeight : " + measuredHeight);
//        Log.v(TAG, "\t>> itemDiff : " + itemDiff);
//        Log.v(TAG, "\t>> totalScrollY : " + totalScrollY);
//        Log.v(TAG, "\t>> firstLineY : " + firstLineY);
//        Log.v(TAG, "\t>> secondLineY : " + secondLineY);
//        Log.v(TAG, "\t>> itemHeight : " + itemHeight);

        counter = 0;
        while (counter < itemsVisible) {
            String contentText = visibles[counter];

            measuredCenterContentStart(contentText);
            measuredOutContentStart(contentText);

            float translateY = itemHeight * counter - itemHeightOffset;

//            Log.v(TAG, "\t>> counter : " + counter + ", translateY : " + translateY + ", contentText : " + contentText);

            canvas.save();

            canvas.translate(0f, translateY);

            if (translateY < firstLineY && translateY + itemHeight > firstLineY) {
                canvas.save();
                canvas.clipRect(0, 0, measuredWidth, firstLineY - translateY);
                canvas.drawText(contentText, drawOutContentStart, (itemHeight + itemOutTextHeight) / 2f, paintOuterText);
                canvas.restore();
                canvas.save();
                canvas.clipRect(0, firstLineY - translateY, measuredWidth, itemHeight);
                canvas.drawText(contentText, drawCenterContentStart, (itemHeight + itemCenterTextHeight) / 2f, paintCenterText);
                canvas.restore();
            } else if (translateY < secondLineY && translateY + itemHeight > secondLineY) {
                canvas.save();
                canvas.clipRect(0, 0, measuredWidth, secondLineY - translateY);
                canvas.drawText(contentText, drawCenterContentStart, (itemHeight + itemCenterTextHeight) / 2f, paintCenterText);
                canvas.restore();
                canvas.save();
                canvas.clipRect(0, secondLineY - translateY, measuredWidth, itemHeight);
                canvas.drawText(contentText, drawOutContentStart, (itemHeight + itemOutTextHeight) / 2f, paintOuterText);
                canvas.restore();
            } else if (translateY >= firstLineY && translateY + itemHeight <= secondLineY) {
                canvas.drawText(contentText, drawCenterContentStart, (itemHeight + itemCenterTextHeight) / 2f, paintCenterText);
            } else {
                canvas.drawText(contentText, drawOutContentStart, (itemHeight + itemOutTextHeight) / 2f, paintOuterText);
            }

            canvas.restore();

            counter++;
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean eventConsumed = gestureDetector.onTouchEvent(event);
        boolean isIgnore = false;

        float top = getTopScrollY();
        float bottom = getBottomScrollY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startTime = System.currentTimeMillis();
                cancelObjectAnimator();
                previousY = event.getRawY();
                break;
            case MotionEvent.ACTION_MOVE:
                float dy = previousY - event.getRawY();
                previousY = event.getRawY();
                totalScrollY += dy;
//                if ((totalScrollY + itemHeight < top && dy < 0)
//                        || (totalScrollY - itemHeight > bottom && dy > 0)) {
                if ((totalScrollY < top && dy < 0)
                        || (totalScrollY > bottom && dy > 0)) {
                    totalScrollY -= dy;
                    isIgnore = true;
                } else {
                    isIgnore = false;
                }
                break;
            case MotionEvent.ACTION_UP:
            default:
                if (!eventConsumed) {
                    if ((System.currentTimeMillis() - startTime) > CLICK_INTERVAL) {
                        scrollBy(getDangleOffset(totalScrollY));
                    } else {
                        float y = event.getY();
                        float offset = getDangleOffset(totalScrollY);
                        float clickOffset = 0f;
                        if (y < firstLineY) {
                            clickOffset = -itemHeight * ((int) ((firstLineY - y) / itemHeight) + 1);
                        } else if (y > secondLineY) {
                            clickOffset = itemHeight * ((int) ((y - secondLineY) / itemHeight) + 1);
                        }
                        scrollBy(clickOffset + offset);
                    }
                }
        }

        if (!isIgnore && event.getAction() != MotionEvent.ACTION_DOWN) {
            invalidate();
        }

        return true;
    }

    private void measuredCenterContentStart(String content) {
        Rect rect = new Rect();
        paintCenterText.getTextBounds(content, 0, content.length(), rect);
        drawCenterContentStart = (int) ((measuredWidth - rect.width()) * 0.5);
    }

    private void measuredOutContentStart(String content) {
        Rect rect = new Rect();
        paintOuterText.getTextBounds(content, 0, content.length(), rect);
        drawOutContentStart = (int) ((measuredWidth - rect.width()) * 0.5);
    }

    private float getTopScrollY() {
        return -initPosition * itemHeight;
    }

    private float getBottomScrollY() {
        return (adapter.getItemsCount() - 1 - initPosition) * itemHeight;
    }

    private void setTotalScrollY(float totalScrollY) {
        this.totalScrollY = totalScrollY;
        invalidate();
    }

    private float getDangleOffset(float scrollY) {
        // must be integer
        int offset = (int) ((scrollY % itemHeight + itemHeight) % itemHeight);
        if (offset > itemHeight / 2f) {
            offset = itemHeight - offset;
        } else {
            offset = -offset;
        }
        return offset;
    }

    private void scrollBy(float offset) {
        Log.d(TAG, "++ scrollBy: " + offset);

        cancelObjectAnimator();

        long duration = (long) (Math.min(Math.abs(offset / itemHeight), 1f) * ANIMATION_DURATION);
        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(this, "totalScrollY", totalScrollY, totalScrollY + offset);
        objectAnimator.setDuration(duration);
        objectAnimator.addListener(animatorListenerAdapter);
        objectAnimator.start();
    }

    private void flingBy(float velocityY) {
        Log.d(TAG, "++ flingBy: " + velocityY);

        cancelObjectAnimator();

        long duration = 0L;
        float dest = totalScrollY;
        float value = velocityY;
        if (Math.abs(value) > VELOCITY_FLING_MAX) {
            value = value > 0 ? VELOCITY_FLING_MAX : -VELOCITY_FLING_MAX;
        }
        float top = getTopScrollY();
        float bottom = getBottomScrollY();

        while (true) {
            if (Math.abs(value) >= 0f && Math.abs(value) <= VELOCITY_FLING_INTERVAL) {
                break;
            }

            dest -= (value * VELOCITY_FLING_RATIO);
            duration += VELOCITY_FLING_DURATION;

            if (dest <= top || dest >= bottom) {
                break;
            }

            if (value > 0) {
                value -= (VELOCITY_FLING_INTERVAL * 2);
            } else {
                value += (VELOCITY_FLING_INTERVAL * 2);
            }
        }

        dest = dest + getDangleOffset(dest);

        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(this, "totalScrollY", totalScrollY, dest);
        objectAnimator.setDuration(duration);
        objectAnimator.setInterpolator(new DecelerateInterpolator(VELOCITY_INTERPOLATOR_FACTOR));
        objectAnimator.addListener(animatorListenerAdapter);
        objectAnimator.start();
    }

    private void onItemSelected() {
        int position = getCurrentPosition();
        Log.d(TAG, "onItemSelected: " + position);
        if (onItemSelectListener != null) {
            onItemSelectListener.onItemSelected(position);
        }
    }

    private AnimatorListenerAdapter animatorListenerAdapter = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            onItemSelected();
        }
    };

    private void cancelObjectAnimator() {
        if (objectAnimator != null && !objectAnimator.isRunning()) {
            objectAnimator.cancel();
            objectAnimator = null;
        }
    }

    public void setAdapter(Adapter adapter) {
        this.adapter = adapter;
        invalidate();
    }

    public Adapter getAdapter() {
        return adapter;
    }

    public void setOnItemSelectListener(OnItemSelectListener onItemSelectListener) {
        this.onItemSelectListener = onItemSelectListener;
    }

    public int getCurrentPosition() {
        return (int) (Math.abs(getTopScrollY()) / itemHeight) + (int) (totalScrollY / itemHeight);
    }

    public void setCurrentPosition(int position) {
        if (getAdapter() == null) {
            return;
        }

        if (position < 0) {
            position = 0;
        }
        if (position > adapter.getItemsCount() - 1) {
            position = adapter.getItemsCount() - 1;
        }

        cancelObjectAnimator();

        float dest = getTopScrollY() + (itemHeight * position);
        scrollBy(dest - totalScrollY);
    }
}
