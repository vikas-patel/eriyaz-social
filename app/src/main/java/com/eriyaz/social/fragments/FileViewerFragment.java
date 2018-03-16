package com.eriyaz.social.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.eriyaz.social.R;
import com.eriyaz.social.activities.BaseActivity;
import com.eriyaz.social.activities.CreatePostActivity;
import com.eriyaz.social.model.RecordingItem;
import com.eriyaz.social.utils.ValidationUtil;

import java.util.concurrent.TimeUnit;

/**
 * Created by Daniel on 12/23/2014.
 */
public class FileViewerFragment extends Fragment {
    private static final String ARG_POSITION = "position";
    private static final String LOG_TAG = "FileViewerFragment";
    private static final String ARG_ITEM = "recording_item";

    private RecordingItem item;
    private TextView vName;
    private String filePath;
    private TextView vLength;
    private View cardView;

    protected EditText titleEditText;
    protected EditText descriptionEditText;
    protected Spinner versionSpinner;

    public static FileViewerFragment newInstance(RecordingItem item) {
        FileViewerFragment f = new FileViewerFragment();
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

        // define an on click listener to open PlaybackFragment
        cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    PlaybackFragment playbackFragment =
                            new PlaybackFragment().newInstance(item);
                    android.app.FragmentTransaction transaction = getActivity().getFragmentManager()
                            .beginTransaction();
                    playbackFragment.show(transaction, "dialog_playback");
                    BaseActivity activity = (BaseActivity) getActivity();
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



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_file_viewer, container, false);
        vName = (TextView) v.findViewById(R.id.file_name_text);
        vLength = (TextView) v.findViewById(R.id.file_length_text);
        cardView = v.findViewById(R.id.card_view);
        titleEditText = (EditText) v.findViewById(R.id.titleEditText);
        descriptionEditText = v.findViewById(R.id.descriptionEditText);
        versionSpinner = v.findViewById(R.id.versionSpinner);
        bindData();
        return v;
    }

    protected void attemptCreatePost() {
        // Reset errors.
        titleEditText.setError(null);
        descriptionEditText.setError(null);
        String title = titleEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        String version = versionSpinner.getSelectedItem().toString();
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

        if (!cancel) {
            ((BaseActivity) getActivity()).hideKeyboard();
            ((CreatePostActivity) getActivity()).savePost(title, description, version, filePath);
        } else if (focusView != null) {
            focusView.requestFocus();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Do something that differs the Activity's menu here
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.create_post_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.post:
                if (((BaseActivity) getActivity()).hasInternetConnection()) {
                    attemptCreatePost();
                } else {
                    ((BaseActivity) getActivity()).showSnackBar(R.string.internet_connection_failed);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public RecordingItem getRecordingItem() {
        return item;
    }
}




