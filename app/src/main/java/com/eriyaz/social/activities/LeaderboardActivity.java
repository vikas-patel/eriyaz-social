package com.eriyaz.social.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.eriyaz.social.R;
import com.eriyaz.social.adapters.PostsAdapter;
import com.eriyaz.social.adapters.ProfileAdapter;
import com.eriyaz.social.managers.PostManager;
import com.eriyaz.social.managers.listeners.OnObjectExistListener;
import com.eriyaz.social.model.Post;
import com.eriyaz.social.model.Profile;
import com.eriyaz.social.model.ProfileListResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class LeaderboardActivity extends BaseActivity{
    private ProfileAdapter profileAdapter;
    private List<Profile> profileList=new ArrayList<>();
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
        SwipeRefreshLayout swipeContainer = (SwipeRefreshLayout) findViewById(R.id.swipeContainer);
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        profileAdapter = new ProfileAdapter(this, swipeContainer);
        profileAdapter.setCallback(new ProfileAdapter.Callback() {
            @Override
            public void onItemClick(final Profile profile, final View view) {
                if (!hasInternetConnection()) {
                    showFloatButtonRelatedSnackBar(R.string.internet_connection_failed);
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
                Toast.makeText(LeaderboardActivity.this, message, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onHeaderClick(String header) {
                Log.d("LeaderboardActivity", "inside onClickListener()");

                // Add Collections.sort code to sort based on weeklyRank field of Profile

                profileList=profileAdapter.getProfileList();
                if (profileList != null) {
                    Collections.sort(profileList, new Comparator<Profile>() {
                        @Override
                        public int compare(Profile profile1, Profile profile2) {
                            if (header.equalsIgnoreCase("WeeklyRank")) {
                                if (profile1.getWeeklyRank() < profile2.getWeeklyRank())
                                    return -1;
                                else if (profile1.getWeeklyRank() == profile2.getWeeklyRank())
                                    return 0;
                                else
                                    return 1;
                            }
                            else {
                                if (profile1.getRank() < profile2.getRank())
                                    return -1;
                                else if (profile1.getRank() == profile2.getRank())
                                    return 0;
                                else
                                    return 1;
                            }
                        }
                    });
                }
                profileAdapter.notifyDataSetChanged();
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
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

        if (!hasInternetConnection())
            showFloatButtonRelatedSnackBar(R.string.internet_connection_failed);
    }

    private void openProfileActivity(String userId, View view) {
        Intent intent = new Intent(LeaderboardActivity.this, ProfileActivity.class);
        intent.putExtra(ProfileActivity.USER_ID_EXTRA_KEY, userId);
        startActivity(intent);
    }
}
