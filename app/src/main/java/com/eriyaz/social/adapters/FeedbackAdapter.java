package com.eriyaz.social.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.eriyaz.social.R;
import com.eriyaz.social.activities.FeedbackActivity;
import com.eriyaz.social.adapters.holders.ReplyTextViewHolder;
import com.eriyaz.social.adapters.holders.FeedbackHolder;
import com.eriyaz.social.adapters.holders.ViewHolder;
import com.eriyaz.social.model.ListItem;
import com.eriyaz.social.utils.LogUtil;

import java.util.List;

/**
 * Created by vikas on 21/4/18.
 */

public class FeedbackAdapter extends RecyclerView.Adapter<ViewHolder> {
    private List<ListItem> mItems;
    private Callback callback;

    @Override
    public int getItemViewType(int position) {
        return mItems.get(position).getListItemType();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = null;
        switch (viewType) {
            case ListItem.TEXT:
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.message_list_item, parent, false);
                return new FeedbackHolder(view, callback);
            case ListItem.TEXT_CHILD:
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.child_message_list_item, parent, false);
                return new FeedbackHolder(view, callback);
            case ListItem.EDIT_TEXT:
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.reply_list_item, parent, false);
                return new ReplyTextViewHolder(view, callback);
        }
        return null;
    }

    public void setList(List<ListItem> list) {
        this.mItems = list;
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ListItem item = mItems.get(position);
        LogUtil.logInfo(FeedbackActivity.TAG, item.toString());
        holder.bindData(item);
    }

    public ListItem getItemByPosition(int position) {
        return mItems.get(position);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public interface Callback {
        void onDeleteClick(int position);

        void sendReply(String messageText, String parentId);

        void onAuthorClick(String authorId);
    }
}
