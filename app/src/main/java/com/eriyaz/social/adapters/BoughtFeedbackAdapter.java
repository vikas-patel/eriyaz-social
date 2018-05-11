/*
 *
 * Copyright 2017 Rozdoum
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.eriyaz.social.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.eriyaz.social.R;
import com.eriyaz.social.adapters.holders.BoughtFeedbackViewHolder;
import com.eriyaz.social.model.BoughtFeedback;
import com.eriyaz.social.model.Comment;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by alexey on 10.05.17.
 */

public class BoughtFeedbackAdapter extends RecyclerView.Adapter<BoughtFeedbackViewHolder> {
    private List<BoughtFeedback> list = new ArrayList<>();
    private Callback callback;

    @Override
    public BoughtFeedbackViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.bought_feedback_list_item, parent, false);
        return new BoughtFeedbackViewHolder(view, callback);
    }

    @Override
    public void onBindViewHolder(BoughtFeedbackViewHolder holder, int position) {
        holder.itemView.setLongClickable(true);
        holder.bindData(getItemByPosition(position));
    }

    public BoughtFeedback getItemByPosition(int position) {
        return list.get(position);
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void setList(List<BoughtFeedback> list) {
        this.list = list;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public interface Callback {
        void toggleResolveClick(String postId);

        void onAuthorClick(String authorId);

        void onPostClick(String postId);
    }
}
