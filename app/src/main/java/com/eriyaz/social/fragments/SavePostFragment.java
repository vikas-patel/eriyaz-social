package com.eriyaz.social.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.eriyaz.social.Constants;
import com.eriyaz.social.R;
import com.eriyaz.social.activities.BaseActivity;
import com.eriyaz.social.activities.CreatePostActivity;
import com.eriyaz.social.dialogs.AvatarDialog;
import com.eriyaz.social.model.RecordingItem;
import com.eriyaz.social.utils.ValidationUtil;

import org.w3c.dom.Text;

import java.io.File;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * Created by Daniel on 12/23/2014.
 */
public class SavePostFragment extends Fragment {
    private static final String ARG_POSITION = "position";
    private static final String LOG_TAG = "FileViewerFragment";
    private static final String ARG_ITEM = "recording_item";

    private RecordingItem item;
    private TextView vName;
    private String filePath;
    private TextView vLength;
    private View cardView;
    private ImageView avatarImageView;
    private Button saveButton;
    private Button retryButton;
    private Button postButton;
    private CheckBox anonymousCheckBox;
    private String avatarImageUrl;
    private TextView postLimitErrorTextView;

    protected EditText titleEditText;
    protected EditText descriptionEditText;
    protected EditText nickNameEditText;

    public static SavePostFragment newInstance(RecordingItem item) {
        SavePostFragment f = new SavePostFragment();
        Bundle b = new Bundle();
        b.putParcelable(ARG_ITEM, item);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        item = getArguments().getParcelable(ARG_ITEM);
    }

    private void bindData() {
        long itemDuration = item.getLength();

        long minutes = TimeUnit.MILLISECONDS.toMinutes(itemDuration);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(itemDuration)
                - TimeUnit.MINUTES.toSeconds(minutes);

        vName.setText(item.getName());
        vLength.setText(String.format("%02d:%02d", minutes, seconds));
        filePath = item.getFilePath();

        anonymousCheckBox.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (anonymousCheckBox.isChecked()) {
                    // show avatar selection grid
                    nickNameEditText.setVisibility(View.VISIBLE);
                    avatarImageView.setVisibility(View.VISIBLE);
                    openAvatarSelectionDialog();
                } else {
                    // remove nick name field and image view
                    nickNameEditText.setVisibility(View.GONE);
                    avatarImageView.setVisibility(View.GONE);
                    avatarImageUrl = "";
                }
            }
        });

        avatarImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openAvatarSelectionDialog();
            }
        });

        // define an on click listener to open PlaybackFragment
        cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    RecordPlayFragment playbackFragment =
                            RecordPlayFragment.newInstance(item);
                    FragmentTransaction transaction = getActivity().getSupportFragmentManager()
                            .beginTransaction();
                    playbackFragment.show(transaction, "dialog_playback");
                } catch (Exception e) {
                    Log.e(LOG_TAG, "exception", e);
                }
            }
        });

        titleEditText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (titleEditText.hasFocus() && titleEditText.getError() != null) {
                    titleEditText.setError(null);
                    return true;
                }
                return false;
            }
        });
    }

    private void openAvatarSelectionDialog() {
        AvatarDialog dialog = new AvatarDialog();
        Bundle args = new Bundle();
        dialog.setArguments(args);
        dialog.setTargetFragment(this, Constants.ACTIVITY.AVATAR_DIALOG);
        dialog.show(getFragmentManager(), AvatarDialog.TAG);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            // set image url
            avatarImageUrl = data.getStringExtra(AvatarDialog.AVATAR_IMAGE_URL_EXTRA_KEY);
            Glide.with(getActivity())
                    .load(avatarImageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .crossFade()
                    .into(avatarImageView);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_save_post, container, false);
        vName = (TextView) v.findViewById(R.id.file_name_text);
        vLength = (TextView) v.findViewById(R.id.file_length_text);
        cardView = v.findViewById(R.id.card_view);
        titleEditText = (EditText) v.findViewById(R.id.titleEditText);
        nickNameEditText = v.findViewById(R.id.nickNameEditText);
        descriptionEditText = v.findViewById(R.id.descriptionEditText);
        anonymousCheckBox = v.findViewById(R.id.anonymousCheckboxId);
        avatarImageView = v.findViewById(R.id.avatarImageViewId);
        postLimitErrorTextView = v.findViewById(R.id.post_limit_error_text);
        retryButton = v.findViewById(R.id.retryButton);
        retryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRetryClick();
            }
        });
        saveButton = v.findViewById(R.id.saveLaterButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSaveLaterClick();
            }
        });
        postButton = v.findViewById(R.id.postButton);
        postButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (((BaseActivity) getActivity()).hasInternetConnection()) {
                    attemptCreatePost();
                } else {
                    ((BaseActivity) getActivity()).showSnackBar(R.string.internet_connection_failed);
                }
            }
        });
        bindData();
        return v;
    }

    protected void attemptCreatePost() {
        // Reset errors.
        titleEditText.setError(null);
        descriptionEditText.setError(null);
        nickNameEditText.setError(null);
        String title = titleEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        String nickName = nickNameEditText.getText().toString().trim();
        View focusView = null;
        boolean cancel = false;

        if (TextUtils.isEmpty(title)) {
            titleEditText.setError(getString(R.string.warning_empty_title));
            focusView = titleEditText;
            cancel = true;
        } else if (!ValidationUtil.isPostTitleValid(title)) {
            titleEditText.setError(getString(R.string.error_post_title_length));
            focusView = titleEditText;
            cancel = true;
        }

        if (!TextUtils.isEmpty(description) && !ValidationUtil.isPostDescriptionValid(description)) {
            descriptionEditText.setError(getString(R.string.error_post_description_length));
            focusView = descriptionEditText;
            cancel = true;
        }

        if (anonymousCheckBox.isChecked() && TextUtils.isEmpty(nickName)) {
            nickNameEditText.setError("Nick name is required for anonymous post");
            focusView = anonymousCheckBox;
            cancel = true;
        }

        CreatePostActivity rootActivity = (CreatePostActivity) getActivity();
        long lastPostDate = rootActivity.getProfile().getLastPostCreatedDate();
        long currentTime = Calendar.getInstance().getTimeInMillis();
        // 4 hrs
        long minInterval = 4*DateUtils.HOUR_IN_MILLIS;
        if (currentTime - lastPostDate < minInterval) {
            cancel = true;
            long hours = (lastPostDate + minInterval - currentTime)/(1000 * 60 * 60);
            long mins = (((lastPostDate + minInterval - currentTime))/(1000*60)) % 60;
            String nextPostDateText = "";
            if (hours > 0) {
                nextPostDateText = hours + " hours ";
            }
            if (mins > 0) {
                nextPostDateText = nextPostDateText +  mins + " minutes";
            }

            postLimitErrorTextView.setText(String.format(getString(R.string.post_limit_error), nextPostDateText));
            postLimitErrorTextView.setVisibility(View.VISIBLE);
        }

        if (!cancel) {
            ((BaseActivity) getActivity()).hideKeyboard();
            ((CreatePostActivity) getActivity()).savePost(item, title, description, filePath, item.getLength(),
                    anonymousCheckBox.isChecked(), nickName, avatarImageUrl);
        } else if (focusView != null) {
            focusView.requestFocus();
        }
    }

    private void onRetryClick() {
        File file = new File(item.getFilePath());
        file.delete();
        CreatePostActivity activity = (CreatePostActivity) getActivity();
        activity.startRecordFragment();
    }

    private void onSaveLaterClick() {
        // Reset errors.
        titleEditText.setError(null);
        String title = titleEditText.getText().toString().trim();
        View focusView = null;
        boolean cancel = false;

        if (TextUtils.isEmpty(title)) {
            titleEditText.setError(getString(R.string.warning_empty_title));
            focusView = titleEditText;
            cancel = true;
        } else if (!ValidationUtil.isPostTitleValid(title)) {
            titleEditText.setError(getString(R.string.error_post_title_length));
            focusView = titleEditText;
            cancel = true;
        }
        if (cancel) {
            focusView.requestFocus();
            return;
        }
        CreatePostActivity activity = (CreatePostActivity) getActivity();
        item.setName(title);
        activity.saveRecording(item);
    }
}




