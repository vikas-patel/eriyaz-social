package com.eriyaz.social.photomovie.widget;

import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;
import android.view.View;

import com.eriyaz.social.R;

/**
 * Created by huangwei on 2018/9/9.
 */
public class MovieBottomView extends ConstraintLayout implements View.OnClickListener {

    private MovieBottomCallback mCallback;

    public MovieBottomView(Context context) {
        super(context);
    }

    public MovieBottomView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MovieBottomView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        findViewById(R.id.movie_next).setOnClickListener(this);
        findViewById(R.id.movie_filter).setOnClickListener(this);
        findViewById(R.id.movie_filter_txt).setOnClickListener(this);
    }

    public void setCallback(MovieBottomCallback callback) {
        this.mCallback = callback;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.movie_filter:
            case R.id.movie_filter_txt:
                if(mCallback!=null){
                    mCallback.onFilterClick();
                }
                break;
            case R.id.movie_next:
                if(mCallback!=null){
                    mCallback.onNextClick();
                }
                break;
        }
    }

    public static interface MovieBottomCallback{
        void onNextClick();
        void onMusicClick();
        void onTransferClick();
        void onFilterClick();
    }
}
