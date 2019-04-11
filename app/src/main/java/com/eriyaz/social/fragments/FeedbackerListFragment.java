package com.eriyaz.social.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.eriyaz.social.R;
import com.eriyaz.social.activities.LeaderboardActivity;
import com.eriyaz.social.activities.ProfileActivity;
import com.eriyaz.social.adapters.ProfileAdapter;
import com.eriyaz.social.model.Profile;

public class FeedbackerListFragment extends Fragment {

    private ProfileAdapter profileAdapter;
    private RecyclerView recyclerView;
    private LeaderboardActivity activity;
    private TextView empty_list_message;
    String queryParameter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View rootView=inflater.inflate(R.layout.activity_leaderboard, container, false);

        final ProgressBar progressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);
        SwipeRefreshLayout swipeContainer = (SwipeRefreshLayout) rootView.findViewById(R.id.swipeContainer);
        empty_list_message=(TextView)rootView.findViewById(R.id.message_leaderboard_list_empty);

        queryParameter=getArguments().getString("orderBy");
        activity=(LeaderboardActivity)getActivity();
        recyclerView = (RecyclerView) rootView.findViewById(R.id.recycler_view);
        profileAdapter = new ProfileAdapter((LeaderboardActivity) (getActivity()), swipeContainer, queryParameter);
        profileAdapter.setCallback(new ProfileAdapter.Callback() {
            @Override
            public void onItemClick(final Profile profile, final View view) {
                if (!activity.hasInternetConnection()) {
                    activity.showFloatButtonRelatedSnackBar(R.string.internet_connection_failed);
                    return;
                }
                openProfileActivity(profile.getId(), view);
            }

            @Override
            public void onListLoadingFinished() {
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCanceled(String message) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getActivity().getApplicationContext(), message, Toast.LENGTH_LONG).show();
            }

            @Override
            public void displayEmptyListMessage(boolean isEmpty) {
                empty_list_message.setVisibility(isEmpty? View.VISIBLE: View.GONE);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
        recyclerView.addItemDecoration(new DividerItemDecoration(recyclerView.getContext(),
                ((LinearLayoutManager) recyclerView.getLayoutManager()).getOrientation()));
        recyclerView.setAdapter(profileAdapter);
        profileAdapter.loadFirstPage();

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });
        if (!activity.hasInternetConnection())
            activity.showFloatButtonRelatedSnackBar(R.string.internet_connection_failed);

        return rootView;
    }

    private void openProfileActivity(String userId, View view) {
        Intent intent = new Intent(getActivity(), ProfileActivity.class);
        intent.putExtra(ProfileActivity.USER_ID_EXTRA_KEY, userId);
        startActivity(intent);
    }
}
