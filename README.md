# DragMoreScrollView
一种浏览图片的交互效果实现，类似iOS相册

![](http://upload-images.jianshu.io/upload_images/2633254-04c2c9b6c551bf71.gif)

### 使用方法:
```
<com.crazypumpkin.library.DragMoreScrollView 
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/sv"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:photoReservedHeight="150dp"
    app:fullAnimationDuration="250"
    android:background="#000000">

    <FrameLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent">

        <com.github.chrisbanes.photoview.PhotoView
            android:id="@+id/iv_detail"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:scaleType="fitCenter"/>

        <FrameLayout
            android:id="@+id/fl_detail_container"
            android:layout_width="match_parent"
            android:layout_height="match_parent">

            <LinearLayout
                android:id="@+id/ll_detail_view"
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:orientation="vertical">
                
                <!-- 详情内容（请按照以上的层级结构，detail_container 只能有一个子View） -->
                
            </LinearLayout>
        </FrameLayout>

    </FrameLayout>
</com.crazypumpkin.library.DragMoreScrollView>
```
### 代码设置：
```
mIvDetail.setOnScaleChangeListener(new OnScaleChangedListener() {
            @Override
            public void onScaleChange(float scaleFactor, float focusX, float focusY) {
                //当图片正在放大浏览的时候，禁用ScrollView
                String scale = mScaleFormat.format(mIvDetail.getScale());
                if (Float.parseFloat(scale) > 1) {
                    mDragMoreScrollView.setEnabled(false);
                } else {
                    mDragMoreScrollView.setEnabled(true);
                }
            }
        });
mDragMoreScrollView.setRatio(1f);//设置图片宽高比
mDragMoreScrollView.setExitZoomRect(new Rect());//设置缩略图的所占区域
//处于详情模式时应禁用图片缩放功能，在退出的时候finish Activity
mDragMoreScrollView.setOnStatusChangeListener(new DragMoreScrollView.OnStatusChangeListener() {
    @Override
    public void onDetailMode() {
        mIvDetail.setZoomable(false);
    }

    @Override
    public void onPhotoMode() {
        mIvDetail.setZoomable(true);
    }

    @Override
    public void onExit() {
        finish();
    }
});
```

