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

import com.eriyaz.social.R;
import com.eriyaz.social.adapters.MessagesAdapter;
import com.eriyaz.social.enums.ProfileStatus;
import com.eriyaz.social.managers.ProfileManager;
import com.eriyaz.social.managers.listeners.OnDataChangedListener;
import com.eriyaz.social.managers.listeners.OnTaskCompleteListener;
import com.eriyaz.social.model.ListItem;
import com.eriyaz.social.model.Message;
import com.eriyaz.social.model.ReplyTextItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class MessageActivity extends BaseActivity {
    public static final String TAG = MessageActivity.class.getSimpleName();
    public static final int CREATE_FEEDBACK = 25;
    protected EditText feedbackEditText;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private EditText messageEditText;

    private String userId;
    private ScrollView scrollView;
    private MessagesAdapter adapter;
    private TextView warningMessagesTextView;
    private boolean attemptToLoadMessages = false;
    private ProfileManager profileManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        profileManager = ProfileManager.getInstance(MessageActivity.this);
        userId = getIntent().getStringExtra(ProfileActivity.USER_ID_EXTRA_KEY);
        scrollView = (ScrollView) findViewById(R.id.scrollView);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        warningMessagesTextView = (TextView) findViewById(R.id.warningMessagesTextView);

        messageEditText = (EditText) findViewById(R.id.messageEditText);
        messageEditText.setHint(R.string.message_text_hint);
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
                        sendMessage();
                    } else {
                        doAuthorization(profileStatus);
                    }
                } else {
                    showSnackBar(R.string.internet_connection_failed);
                }
            }
        });

        loadMessageList();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        profileManager.closeListeners(this);
    }


    private void loadMessageList() {
        if (recyclerView == null) {
            recyclerView = findViewById(R.id.recycler_view);
            adapter = new MessagesAdapter();
            adapter.setCallback(new MessagesAdapter.Callback() {
                @Override
                public void onDeleteClick(int position) {
                    Message selectedMessage = (Message) adapter.getItemByPosition(position);
                    attemptToRemoveMessage(selectedMessage.getId());
                }

                @Override
                public void sendReply(String messageText, String parentId) {
                    ProfileStatus profileStatus = ProfileManager.getInstance(MessageActivity.this).checkProfile();
                    if (profileStatus.equals(ProfileStatus.PROFILE_CREATED)) {
                        Message message = new Message(messageText);
                        message.setParentId(parentId);
                        message.setReceiverId(userId);
                        saveMessage(message);
                        hideKeyBoard();
                    } else {
                        doAuthorization(profileStatus);
                    }
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
            profileManager.getMessagesList(this, userId, createOnMessageChangedDataListener());
        }
    }

    private void openProfileActivity(String userId) {
        if (userId == null || userId.isEmpty()) return;
        Intent intent = new Intent(MessageActivity.this, ProfileActivity.class);
        intent.putExtra(ProfileActivity.USER_ID_EXTRA_KEY, userId);
        startActivity(intent);
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

    private OnDataChangedListener<Message> createOnMessageChangedDataListener() {

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
                adapter.setList(buildList(list));
            }
        };
    }

    private List<ListItem> buildList(List<Message> messages) {
        final HashMap<Message, ArrayList<Message>> parentMessages = new HashMap();
        ArrayList resultList = new ArrayList();
        Iterator<Message> iterator = messages.iterator();
        while (iterator.hasNext()) {
            Message message = iterator.next();
            if (message.getParentId() == null) {
                parentMessages.put(message, new ArrayList<Message>());
                iterator.remove();
            }
        }
        Iterator<Message> iteratorChild = messages.iterator();
        while (iteratorChild.hasNext()) {
            Message child = iteratorChild.next();
            Message parentMessage = new Message();
            parentMessage.setId(child.getParentId());
            ArrayList<Message> parentHolder = parentMessages.get(parentMessage);
            if (parentHolder != null) parentHolder.add(child);
        }

        ArrayList<Message> keyList = new ArrayList<>(parentMessages.keySet());
        Collections.sort(keyList, new Comparator<Message>() {
            @Override
            public int compare(Message lhs, Message rhs) {
                List<Message> lChildren = parentMessages.get(lhs);
                List<Message> rChildren = parentMessages.get(rhs);
                long latestL = lChildren.isEmpty()?lhs.getCreatedDate():lChildren.get(0).getCreatedDate();
                long latestR = rChildren.isEmpty()?rhs.getCreatedDate():rChildren.get(0).getCreatedDate();
                return ((Long) latestR).compareTo((Long) latestL);
            }
        });

        for (Message key: keyList) {
            List<Message> children = parentMessages.get(key);
            ReplyTextItem replyItem = new ReplyTextItem(key.getId());
            resultList.add(key);
            resultList.add(replyItem);
            resultList.addAll(children);
        }
        return resultList;
    }

    /**
     * Report an issue, suggest a feature, or send message.
     */
    private void sendMessage() {
        String messageText = messageEditText.getText().toString();

        if (messageText.length() > 0) {
            Message message = new Message(messageText);
            message.setReceiverId(userId);
            saveMessage(message);
            messageEditText.setText(null);
            messageEditText.clearFocus();
            hideKeyBoard();
        }
    }

    private void saveMessage(Message message) {
        profileManager.createOrUpdateMessage(message, new OnTaskCompleteListener() {
            @Override
            public void onTaskComplete(boolean success) {
                if (success) {
                    scrollToFirstComment();
                }
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
}
