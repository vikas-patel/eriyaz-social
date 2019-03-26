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

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.eriyaz.social.R;
import com.eriyaz.social.adapters.holders.RatingViewHolder;
import com.eriyaz.social.model.Post;
import com.eriyaz.social.model.Rating;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by alexey on 10.05.17.
 */

public class RatingsAdapter extends RecyclerView.Adapter<RatingViewHolder> {
    private List<Rating> list = new ArrayList<>();
    private Callback callback;
    private Post post;

    public RatingsAdapter(Post aPost) {
        post = aPost;
    }

    @Override
    public RatingViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.rating_list_item, parent, false);
        return new RatingViewHolder(view, callback);
    }

    @Override
    public void onBindViewHolder(RatingViewHolder holder, int position) {
        holder.itemView.setLongClickable(true);
        holder.bindData(getItemByPosition(position), post);
    }

    public Rating getItemByPosition(int position) {
        return list.get(position);
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void setList(List<Rating> list) {
        boolean notify = true;
        // don't notify when rating is made visible by post author.
        if (this.list != null && this.list.equals(list)) notify = false;
        this.list = list;
        if (notify) notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
            return list.size();
    }

    public interface Callback {
        void onReportClick(View view, int position);

        void onBlockClick(View view, int position);

        void onRemoveRatingClick(View v, int position);

        void onAuthorClick(String authorId, View view);

        void makeRatingVisible(int position);

        void onReplyClick(int position);

        void onRequestFeedbackClick(int position);
    }
}
