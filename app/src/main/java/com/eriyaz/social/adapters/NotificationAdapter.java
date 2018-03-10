package com.eriyaz.social.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.eriyaz.social.R;
import com.eriyaz.social.adapters.holders.NotificationHolder;
import com.eriyaz.social.model.Notification;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by vikas on 12/2/18.
 */

public class NotificationAdapter extends RecyclerView.Adapter<NotificationHolder> {
    private List<Notification> list = new ArrayList<>();
    @Override
    public NotificationHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.notification_list_item, parent, false);
        return new NotificationHolder(view);
    }

    @Override
    public void onBindViewHolder(NotificationHolder holder, int position) {
        holder.bindData(getItemByPosition(position));
    }

    public Notification getItemByPosition(int position) {
        return list.get(position);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public void setList(List<Notification> list) {
        this.list = list;
        notifyDataSetChanged();
    }
}