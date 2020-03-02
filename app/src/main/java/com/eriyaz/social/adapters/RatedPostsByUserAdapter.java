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
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.eriyaz.social.R;
import com.eriyaz.social.activities.BaseActivity;
import com.eriyaz.social.adapters.holders.LoadViewHolder;
import com.eriyaz.social.adapters.holders.NotificationHolder;
import com.eriyaz.social.adapters.holders.RatedPostViewHolder;
import com.eriyaz.social.enums.ItemType;
import com.eriyaz.social.fragments.PlaybackFragment;
import com.eriyaz.social.managers.PostManager;
import com.eriyaz.social.managers.listeners.OnDataChangedListener;
import com.eriyaz.social.managers.listeners.OnObjectChangedListener;
import com.eriyaz.social.model.ItemListResult;
import com.eriyaz.social.model.Post;
import com.eriyaz.social.model.Rating;
import com.eriyaz.social.model.RecordingItem;
import com.eriyaz.social.utils.LogUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;


public class RatedPostsByUserAdapter extends BaseItemsAdapter implements ProfileTabInterface {
    public static final String TAG = RatedPostsByUserAdapter.class.getSimpleName();
    protected BaseActivity activity;
    protected String userId;
    protected int selectedPostPosition = -1;
    protected PostsByUserAdapter.CallBack callBack;

    public RatedPostsByUserAdapter(final BaseActivity activity, String userId, SwipeRefreshLayout swipeContainer) {
        super(activity, swipeContainer);
        this.userId = userId;
        this.activity = activity;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == ItemType.ITEM.getTypeCode()) {
            View view = inflater.inflate(R.layout.post_item_list_view, parent, false);
            return new RatedPostViewHolder(view , createOnClickListener());
        } else {
            return new LoadViewHolder(inflater.inflate(R.layout.loading_view, parent, false));
        }
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
            public void onPlayClick(int position, Post post, Rating rating, String authorName, View view) {
                selectedPostPosition = position;
                try {
                    RecordingItem item = new RecordingItem();
                    item.setName(post.getTitle());
                    item.setLength(post.getAudioDuration());
                    item.setFilePath(post.getImagePath());
                    PlaybackFragment playbackFragment =
                            new PlaybackFragment().newInstance(item, post, rating, authorName,false);
                    FragmentTransaction transaction = activity.getSupportFragmentManager()
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
                itemList.set(postPosition, rating);
                notifyItemChanged(postPosition);
            }
        };
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (position >= getItemCount() - 1 && isMoreDataAvailable && !isLoading) {
            android.os.Handler mHandler = activity.getWindow().getDecorView().getHandler();
            mHandler.post(new Runnable() {
                public void run() {
                    //change adapter contents
                    if (activity.hasInternetConnection()) {
                        isLoading = true;
                        itemList.add(new Rating(ItemType.LOAD));
                        notifyItemInserted(itemList.size());
                        loadNext(lastLoadedItemCreatedDate - 1);
                    } else {
                        activity.showFloatButtonRelatedSnackBar(R.string.internet_connection_failed);
                    }
                }
            });
        }

        if (getItemViewType(position) != ItemType.LOAD.getTypeCode()) {
            ((RatedPostViewHolder) holder).bindData((Rating) itemList.get(position));
        }
    }

    public void loadNext(final long nextItemCreatedDate) {

        if (!activity.hasInternetConnection()) {
            activity.showFloatButtonRelatedSnackBar(R.string.internet_connection_failed);
            hideProgress();
            callBack.onListLoadingFinished();
            return;
        }

        OnObjectChangedListener<ItemListResult> onUserRatingsChangedListener = new OnObjectChangedListener<ItemListResult>() {
            @Override
            public void onObjectChanged(ItemListResult result) {
                lastLoadedItemCreatedDate = result.getLastItemCreatedDate();
                isMoreDataAvailable = result.isMoreDataAvailable();
                List list = result.getItems();

                if (nextItemCreatedDate == 0) {
                    itemList.clear();
                    notifyDataSetChanged();
                    swipeContainer.setRefreshing(false);
                }

                hideProgress();

                if (!list.isEmpty()) {
                    addList(list);
                } else {
                    isLoading = false;
                }

                callBack.onListLoadingFinished();
            }
        };

        PostManager.getInstance(activity).getRatingsListByUser(onUserRatingsChangedListener, nextItemCreatedDate, userId);
    }

    public void removeSelectedPost() {
        itemList.remove(selectedPostPosition);
        callBack.onPostsListChanged(itemList.size());
        notifyItemRemoved(selectedPostPosition);
    }

    public void updateSelectedPost() {
        if (selectedPostPosition != -1) {
            Rating selectedRating = (Rating) getItemByPosition(selectedPostPosition);
            if (selectedRating == null) return;
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

    @Override
    public long getItemId(int position) {
        if (getItemByPosition(position) == null) return -1;
        return ((Rating)getItemByPosition(position)).getId().hashCode();
    }

    @Override
    public int getItemViewType(int position) {
        if (itemList.get(position) == null) return -1;
        return ((Rating)itemList.get(position)).getItemType().getTypeCode();
    }
}
