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

import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import com.eriyaz.social.R;
import com.eriyaz.social.activities.BaseActivity;
import com.eriyaz.social.enums.ItemType;
import com.eriyaz.social.fragments.PlaybackFragment;
import com.eriyaz.social.managers.PostManager;
import com.eriyaz.social.managers.listeners.OnPostChangedListener;
import com.eriyaz.social.model.Post;
import com.eriyaz.social.model.Rating;
import com.eriyaz.social.model.RecordingItem;
import com.eriyaz.social.utils.LogUtil;

import java.util.LinkedList;
import java.util.List;

public abstract class BaseItemsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final String TAG = BaseItemsAdapter.class.getSimpleName();

    protected List itemList = new LinkedList<>();
    protected BaseActivity activity;
    protected boolean isLoading = false;
    protected boolean isMoreDataAvailable = true;
    protected long lastLoadedItemCreatedDate;
    protected SwipeRefreshLayout swipeContainer;

    public BaseItemsAdapter(BaseActivity activity, SwipeRefreshLayout swipeContainer) {
        this.activity = activity;
        this.swipeContainer = swipeContainer;
        initRefreshLayout();
        setHasStableIds(true);
    }

    abstract void loadNext(long nextItemCreatedDate);

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
        } else {
            swipeContainer.setRefreshing(false);
            activity.showFloatButtonRelatedSnackBar(R.string.internet_connection_failed);
        }
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    protected Object getItemByPosition(int position) {
        return itemList.get(position);
    }

    public void loadFirstPage() {
        loadNext(0);
    }

    protected void addList(List list) {
        this.itemList.addAll(list);
        notifyDataSetChanged();
        isLoading = false;
    }

    protected void hideProgress() {
        if (!itemList.isEmpty() && getItemViewType(itemList.size() - 1) == ItemType.LOAD.getTypeCode()) {
            itemList.remove(itemList.size() - 1);
            notifyItemRemoved(itemList.size() - 1);
        }
    }
}
