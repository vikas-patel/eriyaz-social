package com.eriyaz.social.adapters.holders;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.eriyaz.social.model.ListItem;

/**
 * Created by vikas on 21/4/18.
 */

public abstract class ViewHolder extends RecyclerView.ViewHolder {
    public ViewHolder(View itemView) {
        super(itemView);
    }

    public abstract void bindData(ListItem item);
}