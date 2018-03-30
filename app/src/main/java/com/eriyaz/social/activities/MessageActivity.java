package com.eriyaz.social.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.eriyaz.social.R;
import com.eriyaz.social.adapters.MessagesAdapter;
import com.eriyaz.social.enums.ProfileStatus;
import com.eriyaz.social.managers.ProfileManager;
import com.eriyaz.social.managers.listeners.OnDataChangedListener;
import com.eriyaz.social.managers.listeners.OnObjectChangedListener;
import com.eriyaz.social.managers.listeners.OnTaskCompleteListener;
import com.eriyaz.social.model.Message;
import com.eriyaz.social.model.Profile;

import java.util.List;

/**
 * Created by vikas on 13/2/18.
 */

public class MessageActivity extends BaseActivity {
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private EditText messageEditText;

    private String userId;
    private ScrollView scrollView;
    private MessagesAdapter adapter;
//    private SwipeRefreshLayout swipeContainer;
    private ProfileManager profileManager;
    private TextView warningMessagesTextView;
    private boolean attemptToLoadMessages = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        userId = getIntent().getStringExtra(ProfileActivity.USER_ID_EXTRA_KEY);
        profileManager = ProfileManager.getInstance(this);
        scrollView = (ScrollView) findViewById(R.id.scrollView);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        warningMessagesTextView = (TextView) findViewById(R.id.warningMessagesTextView);

//        swipeContainer = (SwipeRefreshLayout) findViewById(R.id.swipeContainer);
//        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
//            @Override
//            public void onRefresh() {
//                onRefreshAction();
//            }
//        });

        messageEditText = (EditText) findViewById(R.id.messageEditText);
        final Button sendButton = (Button) findViewById(R.id.sendButton);

        messageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                sendButton.setEnabled(charSequence.toString().trim().length() > 0);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (hasInternetConnection()) {
                    ProfileStatus profileStatus = ProfileManager.getInstance(MessageActivity.this).checkProfile();

                    if (profileStatus.equals(ProfileStatus.PROFILE_CREATED)) {
                        sendComment();
                    } else {
                        doAuthorization(profileStatus);
                    }
                } else {
                    showSnackBar(R.string.internet_connection_failed);
                }
            }
        });


        loadMessageList();
        profileManager.getProfileSingleValue(userId, createProfileChangeListener());
        supportPostponeEnterTransition();
    }

    private OnObjectChangedListener<Profile> createProfileChangeListener() {
        return new OnObjectChangedListener<Profile>() {
            @Override
            public void onObjectChanged(Profile obj) {
                if (isActivityDestroyed()) return;
                if (obj != null && actionBar != null) {
                    actionBar.setTitle(obj.getUsername() + " " + getString(R.string.title_activity_messages));
                }
            }
        };
    }

    private void onRefreshAction() {
        profileManager.getMessagesList(this, userId, createOnMessagesChangedDataListener());
    }

    private void loadMessageList() {
        if (recyclerView == null) {
            recyclerView = findViewById(R.id.recycler_view);
            adapter = new MessagesAdapter(userId);
            adapter.setCallback(new MessagesAdapter.Callback() {
                @Override
                public void onDeleteClick(int position) {
                    Message selectedMessage = adapter.getItemByPosition(position);
                    attemptToRemoveMessage(selectedMessage.getId());
                }

                @Override
                public void onReplyClick(int position) {
                    Message selectedMessage = adapter.getItemByPosition(position);
                    openSenderMessageActivity(selectedMessage.getSenderId());
                }

                @Override
                public void onAuthorClick(String authorId) {
                    openProfileActivity(authorId);
                }
            });
            recyclerView.setNestedScrollingEnabled(false);
            ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
            recyclerView.setAdapter(adapter);
            recyclerView.addItemDecoration(new DividerItemDecoration(recyclerView.getContext(),
                    ((LinearLayoutManager) recyclerView.getLayoutManager()).getOrientation()));
            profileManager.getMessagesList(this, userId, createOnMessagesChangedDataListener());
        }
    }

    private OnDataChangedListener<Message> createOnMessagesChangedDataListener() {

        attemptToLoadMessages = true;

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (attemptToLoadMessages) {
                    progressBar.setVisibility(View.GONE);
                    warningMessagesTextView.setVisibility(View.VISIBLE);
                }
            }
        }, PostDetailsActivity.TIME_OUT_LOADING_COMMENTS);

        return new OnDataChangedListener<Message>() {
            @Override
            public void onListChanged(List<Message> list) {
                attemptToLoadMessages = false;
//                swipeContainer.setRefreshing(false);
                progressBar.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                warningMessagesTextView.setVisibility(View.GONE);
                adapter.setList(list);
            }
        };
    }

    private void openProfileActivity(String userId) {
        Intent intent = new Intent(MessageActivity.this, ProfileActivity.class);
        intent.putExtra(ProfileActivity.USER_ID_EXTRA_KEY, userId);
        startActivity(intent);
    }

    private void openSenderMessageActivity(String userId) {
        if (hasInternetConnection()) {
            Intent intent = new Intent(MessageActivity.this, MessageActivity.class);
            intent.putExtra(ProfileActivity.USER_ID_EXTRA_KEY, userId);
            startActivity(intent);
        } else {
            showSnackBar(R.string.internet_connection_failed);
        }
    }

    private void attemptToRemoveMessage(String messageId) {
        if (hasInternetConnection()) {
            openConfirmDeletingDialog(messageId);
        } else {
            showSnackBar(R.string.internet_connection_failed);
        }
    }

    private void openConfirmDeletingDialog(final String messageId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.confirm_deletion_message)
                .setNegativeButton(R.string.button_title_cancel, null)
                .setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        removeMessage(messageId);
                    }
                });

        builder.create().show();
    }

    private void removeMessage(String messageId) {
        showProgress();
        profileManager.removeMessage(messageId, userId, new OnTaskCompleteListener() {
            @Override
            public void onTaskComplete(boolean success) {
                hideProgress();
                showSnackBar(R.string.message_was_removed);
            }
        });
    }

    private void hideKeyBoard() {
        // Check if no view has focus:
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void scrollToFirstComment() {
        scrollView.smoothScrollTo(0, 0);
    }

    private void sendComment() {

        String messageText = messageEditText.getText().toString();

        if (messageText.length() > 0) {
            profileManager.createOrUpdateMessage(messageText, userId, new OnTaskCompleteListener() {
                @Override
                public void onTaskComplete(boolean success) {
                    if (success) {
                        scrollToFirstComment();
                    }
                }
            });
            messageEditText.setText(null);
            messageEditText.clearFocus();
            hideKeyBoard();
        }
    }
}
