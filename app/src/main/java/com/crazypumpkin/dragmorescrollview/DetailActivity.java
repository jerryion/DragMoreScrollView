package com.crazypumpkin.dragmorescrollview;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.crazypumpkin.library.DragMoreScrollView;
import com.github.chrisbanes.photoview.OnScaleChangedListener;
import com.github.chrisbanes.photoview.PhotoView;

import java.text.DecimalFormat;

/**
 * Created by CrazyPumPkin on 2017/6/25.
 */

public class DetailActivity extends AppCompatActivity {

    public static void start(Context context, Rect rect, int imageResId) {
        Intent intent = new Intent(context, DetailActivity.class);
        intent.putExtra("rect", rect);
        intent.putExtra("imageResId", imageResId);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(context.getResources(), imageResId, options);
        intent.putExtra("ratio", options.outWidth / (float) options.outHeight);
        context.startActivity(intent);
    }

    private DragMoreScrollView mDragMoreScrollView;

    private PhotoView mIvDetail;

    private DecimalFormat mScaleFormat;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        mScaleFormat = new DecimalFormat("#.#");

        mDragMoreScrollView = (DragMoreScrollView) findViewById(R.id.sv);
        mIvDetail = (PhotoView) findViewById(R.id.iv_detail);

        mIvDetail.setImageResource(getIntent().getIntExtra("imageResId", R.drawable.corki_splash_6));
        mIvDetail.setOnScaleChangeListener(new OnScaleChangedListener() {
            @Override
            public void onScaleChange(float scaleFactor, float focusX, float focusY) {
                String scale = mScaleFormat.format(mIvDetail.getScale());
                Log.d("scale",scale);
                if (Float.parseFloat(scale) > 1) {
                    mDragMoreScrollView.setEnabled(false);
                } else {
                    mDragMoreScrollView.setEnabled(true);
                }
            }
        });
        mDragMoreScrollView.setRatio(getIntent().getFloatExtra("ratio", 1.69f));
        mDragMoreScrollView.setExitZoomRect((Rect) getIntent().getParcelableExtra("rect"));
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
    }
}
