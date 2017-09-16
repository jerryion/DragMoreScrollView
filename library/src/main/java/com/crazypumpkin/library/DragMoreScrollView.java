package com.crazypumpkin.library;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ScrollView;

/**
 * 一种浏览图片的交互方式
 * Created by CrazyPumPkin on 2017/6/11.
 */

public class DragMoreScrollView extends ScrollView {

    private final int DEFAULT_RESERVE_HEIGHT = Util.dp2px(150);

    private final Interpolator OVERSHOOT_INTERPOLATOR = new OvershootInterpolator(1f);
    private final Interpolator DECELERATE_INTERPOLATOR = new DecelerateInterpolator();

    private final int DEFAULT_FULL_ANIM_DURATION = 250;

    private final int INTENT_UNKNOW = -1;
    //用户此时的拖动意图为退出浏览
    private final int INTENT_EXIT = 0;
    //切换到详情模式
    private final int INTENT_DETAIL = 1;
    //切换到正常的图片浏览
    private final int INTENT_PHOTO = 2;
    //滚动查看详情
    private final int INTENT_SCROLL = 3;

    //图片模式
    private final int MODE_PHOTO = 0;
    //详情模式
    private final int MODE_DETAIL = 1;

    private OnStatusChangeListener mOnStatusChangeListener;

    //图片
    private View mPhotoView;

    //包裹详情视图的容器
    private ViewGroup mDetailContainer;

    //真正的详情视图
    private View mDetailView;

    //切换到详情模式后，图片在顶部露出的高度
    private int mPhotoReservedHeight;

    //从图片模式切换到详情模式的动画时间
    private int mFullAnimDuration;

    //图片居中，上下空白的高度
    private int mTopBottomEmpty;

    //此次拖动图片的意图
    private int mDragIntent;

    //当前模式
    private int mCurrentMode;

    //记录上次的触摸点
    private float mLastX;
    private float mLastY;

    //退出时应该移动到这个矩形内，进入时应该从这个矩形开始放大
    private Rect mZoomRect;

    //原始状态下的图片宽高
    private int mPhotoWidth;

    private int mPhotoHeight;

    //从图片模式切换到详情模式图片的运动距离
    private int mDistancePhotoToDetail;

    public DragMoreScrollView(Context context) {
        super(context,null);
    }

    public DragMoreScrollView(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public DragMoreScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray ta = context.obtainStyledAttributes(attrs,R.styleable.DragMoreScrollView);
        mPhotoReservedHeight = ta.getDimensionPixelSize(R.styleable.DragMoreScrollView_photoReservedHeight,DEFAULT_RESERVE_HEIGHT);
        mFullAnimDuration = ta.getInteger(R.styleable.DragMoreScrollView_fullAnimationDuration,DEFAULT_FULL_ANIM_DURATION);
        ta.recycle();
        init();
    }

    private void init() {
        mCurrentMode = MODE_PHOTO;
        mDragIntent = INTENT_UNKNOW;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        View temp = getChildAt(0);
        if (!(temp instanceof ViewGroup)) {
            throw new IllegalArgumentException("child must be a ViewGroup");
        }
        ViewGroup wrapper = (ViewGroup) temp;
        mPhotoView = wrapper.getChildAt(0);
        temp = wrapper.getChildAt(1);
        if(!(temp instanceof ViewGroup)){
            throw new IllegalArgumentException("detailContainer must be a ViewGroup");
        }
        mDetailContainer = (ViewGroup) temp;
        if(mDetailContainer.getChildCount() > 1){
            throw new IllegalArgumentException("detailContainer can not has more than one child");
        }
        mDetailView = mDetailContainer.getChildAt(0);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = MeasureSpec.getSize(heightMeasureSpec);
        //图片撑满父布局
        mPhotoView.getLayoutParams().height = height;
        ((MarginLayoutParams) mDetailContainer.getLayoutParams()).topMargin = mPhotoReservedHeight;
        //详情视图最开始位移隐藏起来
        int detailTranslationY = height - mPhotoReservedHeight;
        mDetailContainer.setTranslationY(detailTranslationY);
        mDetailView.setTranslationY(detailTranslationY);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    /**
     * 设置宽高比
     * @param ratio 比例
     */
    public void setRatio(final float ratio) {
        post(new Runnable() {
            @Override
            public void run() {
                //根据是横图型还是竖图型的图片计算出图片在屏幕上占据的大小
                if ((float) getWidth() / getHeight() < ratio) {
                    mPhotoWidth = getWidth();
                    mPhotoHeight = (int) (mPhotoWidth / ratio);
                    mTopBottomEmpty = (getHeight() - mPhotoHeight) / 2;
                } else {
                    mPhotoHeight = getHeight();
                    mPhotoWidth = (int) (mPhotoHeight * ratio);
                    mTopBottomEmpty = 0;
                }
                //从图片模式切换到详情模式的位移距离
                mDistancePhotoToDetail = getHeight() - mTopBottomEmpty - mPhotoReservedHeight;
                zoomToEnter();
            }
        });
    }

    /**
     * 设置拖动退出时缩放和位移的最终区域
     * @param rect
     */
    public void setZoomRect(Rect rect) {
        mZoomRect = rect;
    }

    @Override
    protected boolean overScrollBy(int deltaX, int deltaY, int scrollX, int scrollY, int scrollRangeX, int scrollRangeY, int maxOverScrollX, int maxOverScrollY, boolean isTouchEvent) {
        //被禁用时不处理任何事件
        if(!isEnabled()){
            return false;
        }
        switch (mCurrentMode) {
            case MODE_PHOTO:
                switch (mDragIntent) {
                    case INTENT_UNKNOW:
                        //处于初始化状态，图片随手指的轨迹移动
                        mPhotoView.setTranslationY(-deltaY + mPhotoView.getTranslationY());
                        //上滑去往详情模式，下滑退出浏览
                        if (deltaY < 0) {
                            mDragIntent = INTENT_EXIT;
                        } else {
                            mDragIntent = INTENT_DETAIL;
                            //详情视图容器跟随图片底部
                            mDetailContainer.setTranslationY(-deltaY + mDistancePhotoToDetail);
                            //详情视图在容器内做动画
                            mDetailView.animate().translationY(0).setDuration(mFullAnimDuration).start();
                        }
                        deltaY = 0;
                        break;
                    case INTENT_EXIT:
                        //滑动退出过程中，需要水平位移，所以图片位移交付给onTouchEvent
                        deltaY = 0;
                        break;
                    case INTENT_DETAIL:
                    case INTENT_PHOTO:
                        if (isTouchEvent) {
                            //只处理触摸时间，忽略fling
                            onDrag(deltaY);
                            //假如图片被拖动到中线以下，则drag_intent为INTENT_PHOTO，反之是INTENT_DETAIL
                            if (mPhotoView.getTranslationY() < 0) {
                                mDragIntent = INTENT_DETAIL;
                            } else {
                                mDragIntent = INTENT_PHOTO;
                            }
                        }
                        deltaY = 0;
                        break;
                }
                break;
            case MODE_DETAIL:
                switch (mDragIntent) {
                    case INTENT_UNKNOW:
                        //此时为详情模式的初始状态
                        if (scrollY == 0) {
                            if (isTouchEvent) {
                                if (deltaY < 0) {
                                    //下滑代表想切换到图片模式
                                    mDragIntent = INTENT_PHOTO;
                                    onDrag(deltaY);
                                    deltaY = 0;
                                } else {
                                    //下滑则进入正常滑动浏览详情
                                    mDragIntent = INTENT_SCROLL;
                                }
                            } else {
                                //忽略fling事件
                                deltaY = 0;
                            }
                        }
                        break;
                    case INTENT_SCROLL:
                        //当滑到顶的时候，继续滑则进入图片模式
                        if (isTouchEvent && scrollY == 0 && deltaY < 0) {
                            onDrag(deltaY);
                            mDragIntent = INTENT_PHOTO;
                        }
                        break;
                    case INTENT_DETAIL:
                    case INTENT_PHOTO:
                        onDrag(deltaY);
                        if (Math.abs(mPhotoView.getTranslationY()) >= mDistancePhotoToDetail) {
                            mDragIntent = INTENT_DETAIL;
                        } else {
                            mDragIntent = INTENT_PHOTO;
                        }
                        deltaY = 0;
                        break;
                }
                break;
        }
        return super.overScrollBy(deltaX, deltaY, scrollX, scrollY, scrollRangeX, scrollRangeY, maxOverScrollX, maxOverScrollY, isTouchEvent);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_MOVE:
                //因为overScrollBy()方法只能获取到垂直滑动的信息，所以drag to exit的操作放到这里来
                if (mDragIntent == INTENT_EXIT) {
                    //图片随手指移动，背景跟随距离变化透明度
                    float deltaX = ev.getX() - mLastX;
                    float deltaY = ev.getY() - mLastY;
                    mLastX = ev.getX();
                    mLastY = ev.getY();
                    mPhotoView.setTranslationX(mPhotoView.getTranslationX() + deltaX);
                    mPhotoView.setTranslationY(mPhotoView.getTranslationY() + deltaY);
                    float moveRatio = mPhotoView.getTranslationY() / getHeight();
                    float scale = 1 - moveRatio;
                    if(scale > 1){
                        scale = 1;
                    }else if(scale < 0){
                        scale = 0;
                    }
                    mPhotoView.setScaleX(scale);
                    mPhotoView.setScaleY(scale);
                    int bgAlpha = (int) (255 * (1 - moveRatio));
                    if (bgAlpha > 255) {
                        bgAlpha = 255;
                    } else if (bgAlpha < 0) {
                        bgAlpha = 0;
                    }
                    setBackgroundColor(Color.argb(bgAlpha, 0, 0, 0));
                }
                break;
            case MotionEvent.ACTION_UP:
                //手指抬起，图片根据mode和drag intent去到对应的状态
                switch (mCurrentMode) {
                    case MODE_PHOTO:
                        if (mDragIntent == INTENT_EXIT) {
                            if(mPhotoView.getTranslationY()>0){
                                zoomToExit();
                            }else{
                                toPhotoMode();
                            }
                        } else if (mDragIntent == INTENT_DETAIL) {
                            toDetailMode();
                        } else if (mDragIntent == INTENT_PHOTO) {
                            toPhotoMode();
                        }
                        break;
                    case MODE_DETAIL:
                        if (mDragIntent == INTENT_DETAIL) {
                            toDetailMode();
                        } else if (mDragIntent == INTENT_PHOTO) {
                            toPhotoMode();
                        }
                        break;
                }
                break;
        }
        return super.onTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastX = ev.getX();
                mLastY = ev.getY();
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    /**
     * 图片和详情一齐位移
     * @param deltaY
     */
    private void onDrag(int deltaY) {
        mPhotoView.setTranslationY(-deltaY + mPhotoView.getTranslationY());
        mDetailContainer.setTranslationY(-deltaY + mDetailContainer.getTranslationY());
    }

    /**
     * 拖动图片退出
     */
    public void zoomToExit() {
        int translationX = mZoomRect.centerX() - getWidth() / 2;
        int translationY = mZoomRect.centerY() - getHeight() / 2;
        float scale = Math.min(mZoomRect.width() / (float) mPhotoWidth, mZoomRect.height() / (float) mPhotoHeight);

        ObjectAnimator translatioinXAnimator = ObjectAnimator.ofFloat(mPhotoView,"translationX",translationX);
        ObjectAnimator translatioinYAnimator = ObjectAnimator.ofFloat(mPhotoView,"translationY",translationY);
        ObjectAnimator scaleXAnimator = ObjectAnimator.ofFloat(mPhotoView,"scaleX",scale);
        ObjectAnimator scaleYAnimator = ObjectAnimator.ofFloat(mPhotoView,"scaleY",scale);

        ValueAnimator bgAnimator = ValueAnimator.ofInt(((ColorDrawable)getBackground()).getAlpha(),0);
        bgAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                setBackgroundColor(Color.argb((int) animation.getAnimatedValue(),0,0,0));
            }
        });
        AnimatorSet animatorSet = new AnimatorSet().setDuration(mFullAnimDuration);
        animatorSet.setInterpolator(DECELERATE_INTERPOLATOR);
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mOnStatusChangeListener.onExit();
                setVisibility(View.GONE);
            }
        });
        animatorSet.playTogether(translatioinXAnimator,translatioinYAnimator,scaleXAnimator,scaleYAnimator,bgAnimator);
        animatorSet.start();
    }

    /**
     * 放大进入详情
     */
    private void zoomToEnter(){
        int translationX = mZoomRect.centerX() - getWidth() / 2;
        int translationY = mZoomRect.centerY() - getHeight() / 2;
        float scale = Math.min(mZoomRect.width() / (float) mPhotoWidth, mZoomRect.height() / (float) mPhotoHeight);
        setBackgroundColor(Color.argb(0,0,0,0));
        mPhotoView.setTranslationX(translationX);
        mPhotoView.setTranslationY(translationY);
        mPhotoView.setScaleX(scale);
        mPhotoView.setScaleY(scale);

        ObjectAnimator translatioinXAnimator = ObjectAnimator.ofFloat(mPhotoView,"translationX",0);
        ObjectAnimator translatioinYAnimator = ObjectAnimator.ofFloat(mPhotoView,"translationY",0);
        ObjectAnimator scaleXAnimator = ObjectAnimator.ofFloat(mPhotoView,"scaleX",1);
        ObjectAnimator scaleYAnimator = ObjectAnimator.ofFloat(mPhotoView,"scaleY",1);

        ValueAnimator bgAnimator = ValueAnimator.ofInt(0,255);
        bgAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                setBackgroundColor(Color.argb((int) animation.getAnimatedValue(),0,0,0));
            }
        });
        AnimatorSet animatorSet = new AnimatorSet().setDuration(mFullAnimDuration);
        animatorSet.setInterpolator(DECELERATE_INTERPOLATOR);
        animatorSet.playTogether(translatioinXAnimator,translatioinYAnimator,scaleXAnimator,scaleYAnimator,bgAnimator);
        animatorSet.start();

    }

    /**
     * 切换到图片模式
     */
    private void toPhotoMode() {
        mPhotoView.animate().translationY(0).translationX(0).setInterpolator(OVERSHOOT_INTERPOLATOR).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mCurrentMode = MODE_PHOTO;
                mDragIntent = INTENT_UNKNOW;
                if(mOnStatusChangeListener!=null){
                    mOnStatusChangeListener.onPhotoMode();
                }
            }
        }).setDuration(mFullAnimDuration).start();
        int detailTranslationY = getHeight() - mPhotoReservedHeight;
        mDetailContainer.animate().translationY(detailTranslationY).setInterpolator(DECELERATE_INTERPOLATOR).setDuration(mFullAnimDuration).start();
        mDetailView.animate().translationY(detailTranslationY).setInterpolator(DECELERATE_INTERPOLATOR).setDuration(mFullAnimDuration).start();
    }

    /**
     * 切换到详情模式
     */
    private void toDetailMode() {
        mPhotoView.animate().translationY(-mDistancePhotoToDetail).setDuration(getPhotoAnimDuration()).setInterpolator(OVERSHOOT_INTERPOLATOR).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mCurrentMode = MODE_DETAIL;
                mDragIntent = INTENT_UNKNOW;
                if(mOnStatusChangeListener!=null){
                    mOnStatusChangeListener.onDetailMode();
                }
            }
        }).start();
        mDetailContainer.animate().translationY(0).setInterpolator(OVERSHOOT_INTERPOLATOR).setDuration(getPhotoAnimDuration()).start();
    }

    /**
     * 图片模式和详情模式互相切换时，手指离开屏幕后，动画时长
     * @return
     */
    private int getPhotoAnimDuration() {
        return (int) (Math.abs(mDistancePhotoToDetail + mPhotoView.getTranslationY()) / (float) mDistancePhotoToDetail * mFullAnimDuration);
    }

    public void setOnStatusChangeListener(OnStatusChangeListener listener){
        mOnStatusChangeListener = listener;
    }

    public interface OnStatusChangeListener{
        void onDetailMode();
        void onPhotoMode();
        void onExit();
    }
}
