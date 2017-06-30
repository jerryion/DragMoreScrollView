package com.crazypumpkin.dragmorescrollview;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.crazypumpkin.library.Util;

public class MainActivity extends AppCompatActivity {

    private RecyclerView mRvCorki;

    private int[] mImages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mImages = new int[]{
                R.drawable.corki_splash_0,
                R.drawable.corki_splash_1,
                R.drawable.corki_splash_2,
                R.drawable.corki_splash_3,
                R.drawable.corki_splash_4,
                R.drawable.corki_splash_5,
                R.drawable.corki_splash_6,
                R.drawable.corki_splash_7,
                R.drawable.corki_splash_8
        };
        mRvCorki = (RecyclerView) findViewById(R.id.rv_corki);
        mRvCorki.setLayoutManager(new GridLayoutManager(this, 2));
        mRvCorki.setAdapter(new CorkiAdapter());
    }

    private class CorkiAdapter extends RecyclerView.Adapter<CorkiAdapter.CorkiViewHolder> {


        @Override
        public CorkiViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new CorkiViewHolder(getLayoutInflater().inflate(R.layout.item_corki_avatar, parent, false));
        }

        @Override
        public void onBindViewHolder(final CorkiViewHolder holder,int position) {
            final int resId = mImages[position];
            holder.avatar.setImageResource(resId);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DetailActivity.start(MainActivity.this, Util.getViewRect(MainActivity.this, holder.avatar, true), resId);
                }
            });
        }

        @Override
        public int getItemCount() {
            return mImages.length;
        }

        public class CorkiViewHolder extends RecyclerView.ViewHolder {

            ImageView avatar;

            public CorkiViewHolder(View itemView) {
                super(itemView);
                avatar = (ImageView) itemView.findViewById(R.id.iv_avatar);

            }
        }
    }
}
