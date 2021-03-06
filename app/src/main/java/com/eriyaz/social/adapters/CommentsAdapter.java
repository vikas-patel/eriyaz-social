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
import com.eriyaz.social.adapters.holders.CommentViewHolder;
import com.eriyaz.social.controllers.LikeController;
import com.eriyaz.social.model.Comment;
import com.eriyaz.social.model.Post;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by alexey on 10.05.17.
 */

public class CommentsAdapter extends RecyclerView.Adapter<CommentViewHolder> {
    private List<Comment> list = new ArrayList<>();
    private Callback callback;
    private Post post;
    private boolean isAdmin;

    public CommentsAdapter(Post aPost, boolean aIsAdmin) {
        post = aPost;
        isAdmin = aIsAdmin;
        setHasStableIds(true);
    }

    @Override
    public CommentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.comment_list_item, parent, false);
        return new CommentViewHolder(view, callback, isAdmin);
    }

    @Override
    public void onBindViewHolder(CommentViewHolder holder, int position) {
        holder.itemView.setLongClickable(true);
        holder.bindData(getItemByPosition(position), post);
    }

    public Comment getItemByPosition(int position) {
        return list.get(position);
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void setList(List<Comment> list) {
        this.list = list;
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        if (getItemByPosition(position) == null) return -1;
        return getItemByPosition(position).getId().hashCode();
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public interface Callback {
        void onDeleteClick(View view, int position);

        void onLikeClick(LikeController likeController, int position);

        void onEditClick(View view, int position);

        void onRewardClick(View view, int position, int points);

        void onUserRewardClick(View view, int position, int points);

        void onPlayClick(View view, int position, String authorName);

        void onReportClick(View view, int position);

        void onBlockClick(View view, int position);

        void onAuthorClick(String authorId, View view);

        void onLikeUserListClick(int position);

        void onTimeStampClick(String comment, String timestamp);
    }
}
