/*
 *  Copyright 2017 Rozdoum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.eriyaz.social.adapters;

import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.eriyaz.social.R;
import com.eriyaz.social.activities.BaseActivity;
import com.eriyaz.social.activities.RewardActivity;
import com.eriyaz.social.adapters.holders.LoadViewHolder;
import com.eriyaz.social.adapters.holders.NotificationHolder;
import com.eriyaz.social.adapters.holders.PostViewHolder;
import com.eriyaz.social.enums.ItemType;
import com.eriyaz.social.managers.PostManager;
import com.eriyaz.social.managers.ProfileManager;
import com.eriyaz.social.managers.listeners.OnObjectChangedListener;
import com.eriyaz.social.managers.listeners.OnPostListChangedListener;
import com.eriyaz.social.model.ItemListResult;
import com.eriyaz.social.model.Notification;
import com.eriyaz.social.model.Post;
import com.eriyaz.social.model.PostListResult;
import com.eriyaz.social.model.Rating;
import com.eriyaz.social.utils.PreferencesUtil;

import java.util.List;

/**
 * Created by Kristina on 10/31/16.
 */

public class NotificationAdapter extends BaseItemsAdapter {
    public static final String TAG = NotificationAdapter.class.getSimpleName();
    private NotificationAdapter.Callback callback;
    private String userId;

    public NotificationAdapter(String userId, final BaseActivity activity, SwipeRefreshLayout swipeContainer) {
        super(activity, swipeContainer);
        this.userId = userId;
    }

    public void setCallback(NotificationAdapter.Callback callback) {
        this.callback = callback;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == ItemType.ITEM.getTypeCode()) {
            return new NotificationHolder(inflater.inflate(R.layout.notification_list_item, parent, false));
        } else {
            return new LoadViewHolder(inflater.inflate(R.layout.loading_view, parent, false));
        }
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
                        itemList.add(new Notification(ItemType.LOAD));
                        notifyItemInserted(itemList.size());
                        loadNext(lastLoadedItemCreatedDate - 1);
                    } else {
                        activity.showFloatButtonRelatedSnackBar(R.string.internet_connection_failed);
                    }
                }
            });


        }

        if (getItemViewType(position) != ItemType.LOAD.getTypeCode()) {
            ((NotificationHolder) holder).bindData((Notification) itemList.get(position));
        }
    }

    public void loadNext(final long nextItemCreatedDate) {

        if (!activity.hasInternetConnection()) {
            activity.showFloatButtonRelatedSnackBar(R.string.internet_connection_failed);
            hideProgress();
            callback.onListLoadingFinished();
            return;
        }

        OnObjectChangedListener<ItemListResult> onNotificationsDataChangedListener = new OnObjectChangedListener<ItemListResult>() {
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

                callback.onListLoadingFinished();
            }
        };
                new OnPostListChangedListener<Post>() {
            @Override
            public void onListChanged(PostListResult result) {

            }

            @Override
            public void onCanceled(String message) {
                callback.onCanceled(message);
            }
        };
        ProfileManager.getInstance(activity).getNotificationsList(userId, onNotificationsDataChangedListener, nextItemCreatedDate);
    }

    @Override
    public long getItemId(int position) {
        if (getItemByPosition(position) == null) return -1;
        return ((Notification)getItemByPosition(position)).getId().hashCode();
    }

    @Override
    public int getItemViewType(int position) {
        if (itemList.get(position) == null) return -1;
        return ((Notification)itemList.get(position)).getItemType().getTypeCode();
    }

    public interface Callback {
        void onListLoadingFinished();
        void onCanceled(String message);
    }
}
