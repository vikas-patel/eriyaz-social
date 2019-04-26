package com.eriyaz.social.fragments;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.eriyaz.social.R;
import com.eriyaz.social.activities.BaseActivity;
import com.eriyaz.social.activities.PostDetailsActivity;
import com.eriyaz.social.activities.ProfileActivity;
import com.eriyaz.social.adapters.CommentsByUserAdapter;
import com.eriyaz.social.adapters.PostsByUserAdapter;
import com.eriyaz.social.adapters.ProfileTabInterface;
import com.eriyaz.social.adapters.RatedPostsByUserAdapter;
import com.eriyaz.social.adapters.holders.ProfileTabForCommentsInterface;
import com.eriyaz.social.managers.PostManager;
import com.eriyaz.social.managers.listeners.OnObjectExistListener;
import com.eriyaz.social.model.Comment;
import com.eriyaz.social.model.Post;
import com.eriyaz.social.model.RecordingItem;
import com.eriyaz.social.model.UserComment;

/**
 * Use the {@link PostsByUserFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PostsByUserFragment extends BaseFragment {
    private SwipeRefreshLayout swipeContainer;
    private ProgressBar postsProgressBar;
    private RecyclerView recyclerView;
    public static final String TAB_TYPE_KEY = "PostsByUserFragment.TAB_TYPE";
    public static final String RATED_POST_TYPE = "RATED_POST";
    public static final String POST_TYPE = "POST";
    public static final String USER_COMMENTS_TYPE = "COMMENTS";

    private String userID;
    private String typeValue;
    private RecyclerView.Adapter postsAdapter;

    public PostsByUserFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment PostsByUserFragment.
     */
    public static PostsByUserFragment newInstance(String userID, String typeValue) {
        PostsByUserFragment fragment = new PostsByUserFragment();
        Bundle args = new Bundle();
        args.putString(ProfileActivity.USER_ID_EXTRA_KEY, userID);
        args.putString(TAB_TYPE_KEY, typeValue);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userID = getArguments().getString(ProfileActivity.USER_ID_EXTRA_KEY);
            typeValue = getArguments().getString(TAB_TYPE_KEY);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_posts_by_user, container, false);
        postsProgressBar = view.findViewById(R.id.postsProgressBar);

        swipeContainer = view.findViewById(R.id.swipeContainer);
        loadPostsList(view);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
    }


    private void hideLoadingPostsProgressBar() {
        if (postsProgressBar.getVisibility() != View.GONE) {
            postsProgressBar.setVisibility(View.GONE);
        }
    }

    private void loadPostsList(View view) {
        if (recyclerView == null) {

            recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
            if (typeValue.equals(RATED_POST_TYPE)) {
                postsAdapter = new RatedPostsByUserAdapter((BaseActivity) getActivity(), userID, swipeContainer);
                setCallbackForPosts(postsAdapter);
            } else if (typeValue.equals(POST_TYPE)){
                postsAdapter = new PostsByUserAdapter((BaseActivity) getActivity(), userID);
                setCallbackForPosts(postsAdapter);
            }
            else
            {
                postsAdapter = new CommentsByUserAdapter((BaseActivity) getActivity(), userID, swipeContainer);
                setCallbackForComments(postsAdapter);
            }
            recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
            ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
            recyclerView.setAdapter(postsAdapter);
            if (typeValue.equals(RATED_POST_TYPE) || typeValue.equals(POST_TYPE)) {
                ((ProfileTabInterface) postsAdapter).loadFirstPage();
            }
            else
                ((ProfileTabForCommentsInterface)postsAdapter).loadFirstPage();
        }
    }

    private void setCallbackForComments(RecyclerView.Adapter postsAdapter) {
        ((ProfileTabForCommentsInterface)postsAdapter).setCallBack(new CommentsByUserAdapter.CallBack() {
            @Override
            public void onItemClick(String postId, View view) {
                Log.d("onItemClick()","For Comments");
                BaseActivity activity = (BaseActivity) getActivity();
                if (!activity.hasInternetConnection()) {
                    activity.showFloatButtonRelatedSnackBar(R.string.internet_connection_failed);
                    return;
                }
                openPostForComment(postId, view);
            }
            @Override
            public void onListLoadingFinished() {
                Log.d("onListLoadingFinished()","For Comments");
                postsProgressBar.setVisibility(View.GONE);
            }
            @Override
            public void onPlayClick(View view, int position, String authorName) {
                Log.d("onPlayClick()","For Audio Comments");
                UserComment comment = ((CommentsByUserAdapter)postsAdapter).getItemByPosition(position);
                try {
                    RecordingItem item = new RecordingItem(authorName + " voice comment", comment.getAudioPath(), 0);
                    item.setServer(true);
                    RecordPlayFragment playbackFragment =
                            RecordPlayFragment.newInstance(item);
                    FragmentTransaction transaction = getActivity().getSupportFragmentManager()
                            .beginTransaction();
                    playbackFragment.show(transaction, "dialog_playback");
                } catch (Exception e) {
                    Log.e("onPlayClick()", "exception", e);
                }
            }
        });
    }

    private void setCallbackForPosts(RecyclerView.Adapter postsAdapter)
    {
        ((ProfileTabInterface)postsAdapter).setCallBack(new PostsByUserAdapter.CallBack() {
            @Override
            public void onItemClick(final Post post, final View view) {
                BaseActivity activity = (BaseActivity) getActivity();
                if (!activity.hasInternetConnection()) {
                    activity.showFloatButtonRelatedSnackBar(R.string.internet_connection_failed);
                    return;
                }
                openPostDetailsActivity(post, view);
            }

            @Override
            public void onListLoadingFinished() {
                postsProgressBar.setVisibility(View.GONE);
            }

            @Override
            public void onAuthorClick(String authorId, View view) {
                openProfileActivity(authorId, view);
            }

            @Override
            public void onPostsListChanged(int postsCount) {
//                    String postsLabel = getResources().getQuantityString(R.plurals.posts_counter_format, postsCount, postsCount);

//                    pointsCountersTextView.setVisibility(View.VISIBLE);

//                    if (postsCount > 0) {
//                        postsLabelTextView.setVisibility(View.VISIBLE);
//                    }

                swipeContainer.setRefreshing(false);
                hideLoadingPostsProgressBar();
            }

            @Override
            public void onPostLoadingCanceled() {
                swipeContainer.setRefreshing(false);
                hideLoadingPostsProgressBar();
            }
        });
    }

    private void openPostDetailsActivity(Post post, View v) {
        Intent intent = new Intent(getActivity(), PostDetailsActivity.class);
        intent.putExtra(PostDetailsActivity.POST_ID_EXTRA_KEY, post.getId());
        intent.putExtra(PostDetailsActivity.AUTHOR_ANIMATION_NEEDED_EXTRA_KEY, true);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

//            View imageView = v.findViewById(R.id.fileViewContainer);
//
//            ActivityOptions options = ActivityOptions.
//                    makeSceneTransitionAnimation(ProfileActivity.this,
//                            new android.util.Pair<>(imageView, getString(R.string.post_image_transition_name))
//                    );
            getActivity().startActivityForResult(intent, PostDetailsActivity.UPDATE_POST_REQUEST);
        } else {
            getActivity().startActivityForResult(intent, PostDetailsActivity.UPDATE_POST_REQUEST);
        }
    }

    private void openPostForComment(String postId, View v) {
        Intent intent = new Intent(getActivity(), PostDetailsActivity.class);
        intent.putExtra(PostDetailsActivity.POST_ID_EXTRA_KEY, postId);
        intent.putExtra(PostDetailsActivity.AUTHOR_ANIMATION_NEEDED_EXTRA_KEY, true);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

//            View imageView = v.findViewById(R.id.fileViewContainer);
//
//            ActivityOptions options = ActivityOptions.
//                    makeSceneTransitionAnimation(ProfileActivity.this,
//                            new android.util.Pair<>(imageView, getString(R.string.post_image_transition_name))
//                    );
            getActivity().startActivityForResult(intent, PostDetailsActivity.UPDATE_POST_REQUEST);
        } else {
            getActivity().startActivityForResult(intent, PostDetailsActivity.UPDATE_POST_REQUEST);
        }
    }

    private void openProfileActivity(String userId, View view) {
        Intent intent = new Intent(getActivity(), ProfileActivity.class);
        intent.putExtra(ProfileActivity.USER_ID_EXTRA_KEY, userId);
        getActivity().startActivityForResult(intent, ProfileActivity.CREATE_POST_FROM_PROFILE_REQUEST);
    }

    public ProfileTabInterface getPostsAdapter() {
        return (ProfileTabInterface) postsAdapter;
    }
}
