package com.example.bannerlibrary;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Master
 * @create 2018/9/4 18:03
 */
public class BannerView extends RelativeLayout {


    // 圆点指示器的位置，文字在左，圆点在右
    private static final int INDEX_POSITION_RIGHT = 0x100;
    // 圆点指示器的位置，文字在上，圆点在下
    private static final int INDEX_POSITION_BOTTOM = 0x101;


    // 圆点指示器的位置,右侧
    public static final int POSITION_RITGT = 0x00;
    // 圆点指示器的位置,居中
    public static final int POSITION_CENTER = 0x01;
    // 圆点指示器的位置,左侧
    public static final int POSITION_LEFT = 0x02;


    private static final int HANDLE_UPDATE_INDEX = 0;

    private Context iContext;
    private ViewPager iViewPager;
    private ViewPagerAdapter iPagerAdapter;
    private OnItemClickListener iOnItemClickListener;

    //是否可以自动播放
    private boolean mAutoPlayAble = true;
    //是否正在播放
    private boolean mIsAutoPlaying = false;
    //当前加载到第几页
    private int currentIndex = 0;
    //默认背景图
    private int defaultImageResId;
    private Drawable defaultImageDrawable = null;

    //自动轮播的时间间隔(毫秒)
    private int intervalTime = 5000;
    //控制圆点View的形状，未选中的颜色
    private GradientDrawable gradientDrawable;
    //控制圆点View的形状，选中的颜色
    private GradientDrawable gradientDrawableSelected;
    //选中圆点的颜色值，默认为#FF3333
    private int indexColorResId;
    //轮播图需要的数据列表
    private List<BannerBean> bannerBeanList;
    //圆点指示器的位置，提供两种布局
    private int indexPosition = POSITION_CENTER;

    // 存储展示的图片信息的ImageView
    private List<ImageView> imageLists;
    // 小圆点显示器集合
    private List<View> positionList;


    public BannerView setBannerBeanList(List<BannerBean> bannerBeanList) {
        this.bannerBeanList = bannerBeanList;
        return this;
    }


    public BannerView(Context context) {
        this(context, null);
    }

    public BannerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BannerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyle) {
        iContext = context;
        LayoutInflater.from(context).inflate(R.layout.banner_layout, this, true);
        iViewPager = findViewById(R.id.vp_banner);
        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.banner, defStyle, 0);
        if (typedArray != null) {
            defaultImageDrawable = typedArray.getDrawable(R.styleable.banner_defaultImageDrawable);
            intervalTime = typedArray.getInteger(R.styleable.banner_intervalTime, 5000);
            indexPosition = typedArray.getInteger(R.styleable.banner_indexPosition, POSITION_CENTER);
            ColorStateList indexColorList = typedArray.getColorStateList(R.styleable.banner_indexColor);
            if (indexColorList != null) {
                indexColorResId = indexColorList.getColorForState(getDrawableState(), 0);
            }
            typedArray.recycle();
        }
    }

    /**
     * 设置加载图中时显示的默认图片
     *
     * @param defaultImageResId
     * @return
     */
    public BannerView setDefaultImageResId(int defaultImageResId) {
        this.defaultImageResId = defaultImageResId;
        return this;
    }

    /**
     * 设置加载图中时显示的默认图片
     *
     * @param defaultImageDrawable
     * @return
     */
    public BannerView setDefaultImageDrawable(Drawable defaultImageDrawable) {
        this.defaultImageDrawable = defaultImageDrawable;
        return this;
    }

    /**
     * 设置轮播的时间间隔, 毫秒
     *
     * @param intervalTime
     * @return
     */
    public BannerView setIntervalTime(int intervalTime) {
        this.intervalTime = intervalTime;
        return this;
    }

    /**
     * 设置圆点指示器的位置
     *
     * @param indexPosition
     * @return
     */
    public BannerView setIndexPosition(int indexPosition) {
        this.indexPosition = indexPosition;
        return this;
    }

    /**
     * 设置圆点指示器的颜色
     *
     * @param indexColorResId
     * @return
     */
    public BannerView setIndexColor(int indexColorResId) {
        this.indexColorResId = indexColorResId;
        return this;
    }


    public void show() {
        if (bannerBeanList == null || bannerBeanList.size() == 0) {
            return;
        }
        initImageViewList();
        initIndexList();
        iViewPager.addOnPageChangeListener(new OnPageChangeListener());
        iPagerAdapter = new ViewPagerAdapter();
        iViewPager.setAdapter(iPagerAdapter);
        srartPlay();
    }

    /**
     * 初始化ImageView
     */
    private void initImageViewList() {
        try {
            int count = bannerBeanList.size();
            imageLists = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                final ImageView imageView = new ImageView(iContext);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageLists.add(imageView);
                imageView.setOnClickListener(v -> {
                    if (iOnItemClickListener != null) {
                        iOnItemClickListener.onItemClick(getPosition(imageView));
                    }
                });
                if (defaultImageResId != 0) {
                    GlideApp.with(iContext)
                            .load(bannerBeanList.get(i).getLauncher())
                            .placeholder(defaultImageResId)
                            .error(defaultImageResId)
                            .into(imageView);
                } else if (defaultImageDrawable != null) {
                    GlideApp.with(iContext)
                            .load(bannerBeanList.get(i).getLauncher())
                            .placeholder(defaultImageDrawable)
                            .error(defaultImageDrawable)
                            .into(imageView);
                } else {
                    GlideApp.with(iContext).load(bannerBeanList.get(i).getLauncher()).into(imageView);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("ERROR", e.toString());
        }
    }

    /**
     * 初始化圆点指示器
     */
    private void initIndexList() {
        if (imageLists.size() > 1) {
            // TODO 大于一张才显示指示器
            int count = bannerBeanList.size();
            positionList = new ArrayList<>(count);
            LinearLayout iLinearLayout = findViewById(R.id.position_group);
            if (indexPosition == POSITION_RITGT) {
                iLinearLayout.setGravity(Gravity.CENTER | Gravity.RIGHT);
            } else if (indexPosition == POSITION_CENTER) {
                iLinearLayout.setGravity(Gravity.CENTER);
            } else if (indexPosition == POSITION_LEFT) {
                iLinearLayout.setGravity(Gravity.CENTER | Gravity.LEFT);
            }

            //使用GradientDrawable构造圆形控件
            gradientDrawable = new GradientDrawable();
            gradientDrawable.setShape(GradientDrawable.OVAL);
            gradientDrawable.setColor(Color.parseColor("#999999"));
            gradientDrawableSelected = new GradientDrawable();
            gradientDrawableSelected.setShape(GradientDrawable.OVAL);
            if (indexColorResId != 0) {
                gradientDrawableSelected.setColor(indexColorResId);
            } else {
                gradientDrawableSelected.setColor(Color.parseColor("#1c1cd4"));
            }

            for (int i = 0; i < count; i++) {
                View view = new View(iContext);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(16, 16);
                layoutParams.rightMargin = 8;
                view.setLayoutParams(layoutParams);
                if (0 == i) {
                    view.setBackground(gradientDrawableSelected);
                } else {
                    view.setBackground(gradientDrawable);
                }
                view.bringToFront();
                positionList.add(view);
                iLinearLayout.addView(view);
            }
        }
    }

    /**
     * 开始循环
     */
    private void srartPlay() {
        if (imageLists.size() > 1) {
            // TODO 大于一张才自动播放
            if (mAutoPlayAble && !mIsAutoPlaying) {
                mIsAutoPlaying = true;
                mAutoPlayHandler.sendEmptyMessageDelayed(HANDLE_UPDATE_INDEX, intervalTime);
            }
        } else {
            mAutoPlayHandler.removeMessages(HANDLE_UPDATE_INDEX);
        }
    }

    /**
     * 获取所在位置
     *
     * @param imageView
     * @return
     */
    private int getPosition(ImageView imageView) {
        return imageLists.indexOf(imageView);
    }

    /**
     * Adapter
     */
    private class ViewPagerAdapter extends PagerAdapter {

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            position %= imageLists.size();
            if (position < 0) {
                position = imageLists.size() + position;
            }
            ImageView imageView = imageLists.get(position);
            ViewParent viewParent = imageView.getParent();
            if (viewParent != null) {
                ViewGroup parent = (ViewGroup) viewParent;
                parent.removeView(imageView);
            }
            container.addView(imageView);
            return imageView;
        }

        @Override
        public int getCount() {

            if (bannerBeanList.size() == 1) {
                return 1;
            }

            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @Override
        public void restoreState(@Nullable Parcelable state, @Nullable ClassLoader loader) {

        }

        @Nullable
        @Override
        public Parcelable saveState() {
            return null;
        }

        @Override
        public void startUpdate(@NonNull ViewGroup container) {

        }

        @Override
        public void finishUpdate(@NonNull ViewGroup container) {

        }
    }

    /**
     * ChangeListener
     */
    private class OnPageChangeListener implements ViewPager.OnPageChangeListener {

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            currentIndex = position;
            for (int i = 0; i < bannerBeanList.size(); i++) {
                if (position % positionList.size() == i) {
                    positionList.get(i).setBackground(gradientDrawableSelected);
                } else {
                    positionList.get(i).setBackground(gradientDrawable);
                }
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    }

    /**
     * 设置点击事件
     *
     * @param onItemClickListener
     * @return
     */
    public BannerView setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.iOnItemClickListener = onItemClickListener;
        return this;
    }

    /**
     * 自定义点击回调接口
     */
    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    /**
     * 定时循环Handler
     */
    private Handler mAutoPlayHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == HANDLE_UPDATE_INDEX) {
                currentIndex++;
                iViewPager.setCurrentItem(currentIndex);
                mAutoPlayHandler.sendEmptyMessageDelayed(HANDLE_UPDATE_INDEX, intervalTime);
            }
        }
    };


}
