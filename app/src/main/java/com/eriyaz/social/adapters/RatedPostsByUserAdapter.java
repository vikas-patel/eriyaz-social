/*
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
 */

package com.eriyaz.social.adapters;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.eriyaz.social.R;
import com.eriyaz.social.activities.BaseActivity;
import com.eriyaz.social.adapters.holders.RatedPostViewHolder;
import com.eriyaz.social.enums.ItemType;
import com.eriyaz.social.fragments.PlaybackFragment;
import com.eriyaz.social.managers.PostManager;
import com.eriyaz.social.managers.listeners.OnDataChangedListener;
import com.eriyaz.social.managers.listeners.OnObjectChangedListener;
import com.eriyaz.social.model.Post;
import com.eriyaz.social.model.Rating;
import com.eriyaz.social.model.RecordingItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.LinkedList;
import java.util.List;


public class RatedPostsByUserAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements ProfileTabInterface {
    public static final String TAG = RatedPostsByUserAdapter.class.getSimpleName();
    protected List<Rating> ratedPostList = new LinkedList<>();
    protected BaseActivity activity;
    protected String userId;
    protected int selectedPostPosition = -1;
    protected PostsByUserAdapter.CallBack callBack;

    public RatedPostsByUserAdapter(final BaseActivity activity, String userId) {
        this.userId = userId;
        this.activity = activity;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.post_item_list_view, parent, false);

        return new RatedPostViewHolder(view, createOnClickListener());
    }

    public void setCallBack(PostsByUserAdapter.CallBack callBack) {
        this.callBack = callBack;
    }

    private RatedPostViewHolder.OnClickListener createOnClickListener() {
        return new RatedPostViewHolder.OnClickListener() {
            @Override
            public void onItemClick(int position, Post post, View view) {
                if (callBack != null) {
                    selectedPostPosition = position;
                    callBack.onItemClick(post, view);
                }
            }

            @Override
            public void onPlayClick(int position, Post post, Rating rating, View view) {
                selectedPostPosition = position;
                try {
                    RecordingItem item = new RecordingItem();
                    item.setName(post.getTitle());
                    item.setLength(post.getAudioDuration());
                    item.setFilePath(post.getImagePath());
                    PlaybackFragment playbackFragment =
                            new PlaybackFragment().newInstance(item, post, rating);
                    android.app.FragmentTransaction transaction = activity.getFragmentManager()
                            .beginTransaction();
                    playbackFragment.show(transaction, "dialog_playback");
                } catch (Exception e) {
                    Log.e(TAG, "exception", e);
                }
            }

            @Override
            public void onAuthorClick(String authorId, View view) {
                if (callBack != null) {
                    callBack.onAuthorClick(authorId, view);
                }
            }
        };
    }

    private OnObjectChangedListener<Rating> createOnRatingObjectChangedListener(final int postPosition) {
        return new OnObjectChangedListener<Rating>() {
            @Override
            public void onObjectChanged(Rating rating) {
                ratedPostList.set(postPosition, rating);
                notifyItemChanged(postPosition);
            }
        };
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ((RatedPostViewHolder) holder).bindData(ratedPostList.get(position));
    }

    private void setList(List<Rating> list) {
        ratedPostList.clear();
        ratedPostList.addAll(list);
        notifyDataSetChanged();
    }

    public void loadPosts() {
        if (!activity.hasInternetConnection()) {
            activity.showSnackBar(R.string.internet_connection_failed);
            callBack.onPostLoadingCanceled();
            return;
        }

        OnDataChangedListener<Rating> onPostsDataChangedListener = new OnDataChangedListener<Rating>() {
            @Override
            public void onListChanged(List<Rating> list) {
                setList(list);
                callBack.onPostsListChanged(list.size());
            }
        };

        PostManager.getInstance(activity).getRatingsListByUser(onPostsDataChangedListener, userId);
    }

    public void removeSelectedPost() {
        ratedPostList.remove(selectedPostPosition);
        callBack.onPostsListChanged(ratedPostList.size());
        notifyItemRemoved(selectedPostPosition);
    }

    public void updateSelectedPost() {
        if (selectedPostPosition != -1) {
            Rating selectedRating = getItemByPosition(selectedPostPosition);
            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            // update rated post layout (rated value) only visited self profile
            if (firebaseUser != null && firebaseUser.getUid().equals(selectedRating.getAuthorId()))
            {
                PostManager postManager = PostManager.getInstance(activity);
                postManager.getUserRatingSingleValue(firebaseUser.getUid(), selectedRating.getId(), createOnRatingObjectChangedListener(selectedPostPosition));
                return;
            }
            notifyItemChanged(selectedPostPosition);
        }
    }

    protected Rating getItemByPosition(int position) {
        return ratedPostList.get(position);
    }

    @Override
    public int getItemCount() {
        return ratedPostList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return ItemType.ITEM.getTypeCode();
    }
}
