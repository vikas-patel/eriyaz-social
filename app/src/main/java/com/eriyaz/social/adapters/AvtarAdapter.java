package com.eriyaz.social.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.eriyaz.social.R;
import com.eriyaz.social.model.Avatar;
import com.google.android.exoplayer2.util.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by vikas on 27/6/18.
 */

public class AvtarAdapter extends BaseAdapter {

    //Context
    private Context context;
    private List<Avatar> list = new ArrayList<>();
    private Callback callback;


    public AvtarAdapter (Context context){
        this.context = context;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void setList(List<Avatar> list) {
        this.list = list;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        //Creating a linear layout
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        ImageView imageView = new ImageView(context);


        Glide.with(context)
                .load(list.get(position).getImageUrl())
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .crossFade()
                .error(R.drawable.ic_stub)
                .listener(new RequestListener<String, GlideDrawable>() {
                    @Override
                    public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {

                        return false;
                    }
                })
                .into(imageView);

        //Creating a textview to show the title
        TextView textView = new TextView(context);
        if (Util.SDK_INT >= 17) {
            textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        }
        textView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        textView.setText(list.get(position).getName());

        //Scaling the imageview
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setLayoutParams(new GridView.LayoutParams(200,200));
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callback.onItemClick(list.get(position), view);
            }
        });

        //Adding views to the layout
        linearLayout.addView(imageView);
        linearLayout.addView(textView);

        //Returnint the layout
        return linearLayout;
    }

    public interface Callback {
        void onItemClick(Avatar avatar, View view);
    }
}
