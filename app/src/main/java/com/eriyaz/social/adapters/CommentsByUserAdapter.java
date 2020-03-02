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

import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.eriyaz.social.R;
import com.eriyaz.social.activities.BaseActivity;
import com.eriyaz.social.adapters.holders.LoadViewHolder;
import com.eriyaz.social.adapters.holders.ProfileTabForCommentsInterface;
import com.eriyaz.social.adapters.holders.UserCommentsViewHolder;
import com.eriyaz.social.enums.ItemType;
import com.eriyaz.social.managers.PostManager;
import com.eriyaz.social.managers.UserCommentsManager;
import com.eriyaz.social.managers.listeners.OnObjectChangedListener;
import com.eriyaz.social.model.ItemListResult;
import com.eriyaz.social.model.UserComment;

import java.util.ArrayList;
import java.util.List;


public class CommentsByUserAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements ProfileTabForCommentsInterface {
    public static final String TAG = CommentsByUserAdapter.class.getSimpleName();
    protected BaseActivity activity;
    protected String userId;
    protected int selectedPostPosition = -1;
    private boolean isLoading = false;
    private boolean isMoreDataAvailable = true;
    private long lastLoadedItemCreatedDate;
    protected CallBack callback;
    private List<UserComment> commentsList = new ArrayList<>();
    private boolean attemptToLoadComments = false;
    private SwipeRefreshLayout swipeContainer;
    public static final int TIME_OUT_LOADING_COMMENTS = 30000;

    public CommentsByUserAdapter(final BaseActivity activity, String userId, SwipeRefreshLayout swipeContainer) {
        this.userId = userId;
        this.activity = activity;
        this.swipeContainer=swipeContainer;
        initRefreshLayout();
        setHasStableIds(true);
    }

    private void initRefreshLayout() {
        if (swipeContainer != null) {
            this.swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    onRefreshAction();
                }
            });
        }
    }

    private void onRefreshAction() {
        if (activity.hasInternetConnection()) {
            loadFirstPage();
            //cleanSelectedPostInformation();
        } else {
            swipeContainer.setRefreshing(false);
            activity.showFloatButtonRelatedSnackBar(R.string.internet_connection_failed);
        }
    }

    @Override
    public void loadFirstPage() {
        if (lastLoadedItemCreatedDate == 0) {
            loadNext(0);
        }
        else
            loadNext(lastLoadedItemCreatedDate);
        PostManager.getInstance(activity.getApplicationContext()).clearNewPostsCounter();
    }

    @Override
    public void removeSelectedPost() {
        System.out.println("removeSelectedPost() for CommentsByUserAdapter");
    }

    @Override
    public void updateSelectedPost() {
        System.out.println("updateSelectedPost() for CommentsByUserAdapter");
    }

    public void loadNext(final long nextItemCreatedDate) {

        if (!activity.hasInternetConnection()) {
            activity.showFloatButtonRelatedSnackBar(R.string.internet_connection_failed);
            hideProgress();
            callback.onListLoadingFinished();
            return;
        }

        OnObjectChangedListener<ItemListResult> onCommentsDataChangedListener = new OnObjectChangedListener<ItemListResult>() {
            @Override
            public void onObjectChanged(ItemListResult result) {
                lastLoadedItemCreatedDate = result.getLastItemCreatedDate();
                isMoreDataAvailable = result.isMoreDataAvailable();
                List list = result.getItems();
                System.out.println("From onObjectChanged List size--- "+list.size());
                if (nextItemCreatedDate == 0) {
                    commentsList.clear();
                    notifyDataSetChanged();
                    swipeContainer.setRefreshing(false);
                }

                hideProgress();

                if (!list.isEmpty()) {
                    System.out.println("list not empty");
                    addList(list);
                } else {
                    isLoading = false;
                }

                callback.onListLoadingFinished();
            }
        };

        UserCommentsManager.getInstance(activity).getUserCommentsList(onCommentsDataChangedListener, nextItemCreatedDate, userId);

    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == ItemType.ITEM.getTypeCode()) {
            View view = inflater.inflate(R.layout.user_comment_list_item, parent, false);
            return new UserCommentsViewHolder(view,callback, createOnClickListener());
        } else {
            return new LoadViewHolder(inflater.inflate(R.layout.loading_view, parent, false));
        }
    }

    public void setCallBack(CallBack callBack) {
        this.callback = callBack;
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
                        commentsList.add(new UserComment(ItemType.LOAD));
                        notifyItemInserted(commentsList.size());
                        loadNext(lastLoadedItemCreatedDate - 1);
                    } else {
                        activity.showFloatButtonRelatedSnackBar(R.string.internet_connection_failed);
                    }
                }
            });
        }

        if (getItemViewType(position) != ItemType.LOAD.getTypeCode()) {
            System.out.println("inside onBindViewHolder()-- "+getItemByPosition(position).getText());
            ((UserCommentsViewHolder) holder).bindData(getItemByPosition(position));
        }
    }

    private UserCommentsViewHolder.OnClickListener createOnClickListener() {
        return new UserCommentsViewHolder.OnClickListener() {
            @Override
            public void onItemClick(int position, View view) {
                if (callback != null) {
                    selectedPostPosition = position;
                    callback.onItemClick(getItemByPosition(position).getPostId(), view);
                }
            }

            @Override
            public void onPlayClick(View view, int position, String authorName) {
                if (callback != null) {
                    selectedPostPosition = position;
                    callback.onPlayClick(view, selectedPostPosition, authorName);
                }
            }
        };
    }

    private void hideProgress() {
        if (!commentsList.isEmpty() && getItemViewType(commentsList.size() - 1) == ItemType.LOAD.getTypeCode()) {
            commentsList.remove(commentsList.size() - 1);
            notifyItemRemoved(commentsList.size() - 1);
        }
    }

    private void addList(List<UserComment> list) {
        this.commentsList.addAll(list);
        notifyDataSetChanged();
        isLoading = false;
    }

    @Override
    public int getItemCount() {
        return commentsList.size();
    }

    @Override
    public long getItemId(int position) {
        if (getItemByPosition(position) == null) return -1;
        return getItemByPosition(position).getId().hashCode();
    }

    @Override
    public int getItemViewType(int position) {
        System.out.println("from getItemViewType()--- "+commentsList.get(position));
        if (commentsList.get(position) == null) return -1;
        return ((UserComment)commentsList.get(position)).getItemType().getTypeCode();
    }

    public UserComment getItemByPosition(int position) {
        return commentsList.get(position);
    }

    public interface CallBack {
        void onItemClick(String postId, View view);
        void onListLoadingFinished();
        void onPlayClick(View view, int position, String authorName);
    }
}
