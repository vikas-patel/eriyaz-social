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
import android.widget.Toast;

import com.eriyaz.social.R;
import com.eriyaz.social.activities.LeaderboardActivity;
import com.eriyaz.social.adapters.holders.LeaderBoardViewHolder;
import com.eriyaz.social.adapters.holders.LoadViewHolder;
import com.eriyaz.social.enums.ItemType;
import com.eriyaz.social.managers.PostManager;
import com.eriyaz.social.managers.ProfileManager;
import com.eriyaz.social.managers.listeners.OnProfileListChangedListener;
import com.eriyaz.social.model.Profile;
import com.eriyaz.social.model.ProfileListResult;
import com.eriyaz.social.utils.PreferencesUtil;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Kristina on 10/31/16.
 */

public class ProfileAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final String TAG = ProfileAdapter.class.getSimpleName();

    protected List<Profile> profileList = new LinkedList<>();
    private Callback callback;
    private boolean isLoading = false;
    private boolean isMoreDataAvailable = true;
    private int lastLoadedItemRank;
    private SwipeRefreshLayout swipeContainer;
    private LeaderboardActivity activity;
    private String queryParameter;

    public ProfileAdapter(final LeaderboardActivity activity, SwipeRefreshLayout swipeContainer, String queryParameter) {
        this.activity = activity;
        this.swipeContainer = swipeContainer;
        this.queryParameter=queryParameter;
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
        } else {
            swipeContainer.setRefreshing(false);
            activity.showFloatButtonRelatedSnackBar(R.string.internet_connection_failed);
        }
    }

    @Override
    public int getItemCount() {
        return profileList.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (profileList.get(position) == null) return -1;

        return profileList.get(position).getItemType().getTypeCode();
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == ItemType.ITEM.getTypeCode()) {
            return new LeaderBoardViewHolder(inflater.inflate(R.layout.leaderboard_list_item, parent, false),
                    callback, queryParameter);
        }
        else {
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
                        profileList.add(new Profile(ItemType.LOAD));
                        notifyItemInserted(profileList.size());
                        loadNext(lastLoadedItemRank + 1);
                    } else {
                        activity.showFloatButtonRelatedSnackBar(R.string.internet_connection_failed);
                    }
                }
            });
        }

        if (getItemViewType(position) != ItemType.LOAD.getTypeCode()) {
            ((LeaderBoardViewHolder) holder).bindData(profileList.get(position));
        }
    }

    private void addList(List<Profile> list) {
        this.profileList.addAll(list);
        notifyDataSetChanged();
        isLoading = false;
    }

    public void loadFirstPage() {
        loadNext(0);
        PostManager.getInstance(activity.getApplicationContext()).clearNewPostsCounter();
    }

    private void loadNext(final int nextItemRank) {

        if (!activity.hasInternetConnection()) {
            activity.showFloatButtonRelatedSnackBar(R.string.internet_connection_failed);
            hideProgress();
            callback.onListLoadingFinished();
            return;
        }

        OnProfileListChangedListener<Profile> onProfilesDataChangedListener = new OnProfileListChangedListener<Profile>() {
            @Override
            public void onListChanged(ProfileListResult result) {
                lastLoadedItemRank = result.getLastItemRank();
                isMoreDataAvailable = result.isMoreDataAvailable();
                List<Profile> list = result.getProfiles();

                if (nextItemRank == 0) {
                    profileList.clear();
                    notifyDataSetChanged();
                    swipeContainer.setRefreshing(false);
                }

                hideProgress();

                if (!list.isEmpty()) {
                    addList(list);
                    callback.displayEmptyListMessage(false);

                    if (!PreferencesUtil.isPostWasLoadedAtLeastOnce(activity)) {
                        PreferencesUtil.setPostWasLoadedAtLeastOnce(activity, true);
                    }
                } else {
                    isLoading = false;
                    callback.displayEmptyListMessage(true);
                }

                callback.onListLoadingFinished();
            }

            @Override
            public void onCanceled(String message) {
                callback.onCanceled(message);
            }
        };

        ProfileManager.getInstance(activity).getProfilesByRank(onProfilesDataChangedListener, nextItemRank, queryParameter);
    }

    private void hideProgress() {
        if (!profileList.isEmpty() && getItemViewType(profileList.size() - 1) == ItemType.LOAD.getTypeCode()) {
            profileList.remove(profileList.size() - 1);
            notifyItemRemoved(profileList.size() - 1);
        }
    }

    protected Profile getItemByPosition(int position) {

        return profileList.get(position);
    }

    @Override
    public long getItemId(int position) {

        if (getItemByPosition(position) == null) return -1;
        return getItemByPosition(position).getId().hashCode();
    }

    public interface Callback {
        void onItemClick(Profile profile, View view);
        void onListLoadingFinished();
        void onCanceled(String message);
        void displayEmptyListMessage(boolean isEmpty);
    }
}
