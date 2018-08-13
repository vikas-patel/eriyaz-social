package com.eriyaz.social.fragments;

import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.eriyaz.social.R;
import com.eriyaz.social.activities.CreatePostActivity;
import com.eriyaz.social.adapters.FileViewerAdapter;
import com.eriyaz.social.managers.ProfileManager;
import com.eriyaz.social.managers.listeners.OnDataChangedListener;
import com.eriyaz.social.model.RecordingItem;

import java.io.File;
import java.util.List;

/**
 * Created by Daniel on 12/23/2014.
 */
public class SavedRecordingsFragment extends BaseFragment {
    private static final String LOG_TAG = "FileViewerFragment";

    private String userId;
    private FileViewerAdapter mFileViewerAdapter;
    private ProfileManager profileManager;
    private ProgressBar progressBar;

    public static SavedRecordingsFragment newInstance() {
        SavedRecordingsFragment f = new SavedRecordingsFragment();
        Bundle b = new Bundle();
        f.setArguments(b);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        profileManager = ProfileManager.getInstance(getActivity());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        profileManager.closeListeners(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_saved_recordings, container, false);

        RecyclerView mRecyclerView = (RecyclerView) v.findViewById(R.id.recyclerView);
        progressBar = v.findViewById(R.id.progressBar);
        mRecyclerView.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        llm.setOrientation(LinearLayoutManager.VERTICAL);

        //newest to oldest order (database stores from oldest to newest)
        llm.setReverseLayout(true);
        llm.setStackFromEnd(true);

        mRecyclerView.setLayoutManager(llm);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

        mFileViewerAdapter = new FileViewerAdapter(getActivity(), llm);
        mFileViewerAdapter.setCallback(new FileViewerAdapter.Callback() {
            @Override
            public void onPostClick(int position) {
                RecordingItem item = mFileViewerAdapter.getItem(position);
                ((CreatePostActivity)getActivity()).onRecordEnd(item);
            }

            @Override
            public void onDeleteClick(int position) {
                RecordingItem item = mFileViewerAdapter.getItem(position);
                //remove item from database, recyclerview and storage
                //delete file from storage
                File file = new File(item.getFilePath());
                file.delete();

                Toast.makeText(
                        getActivity(),
                        String.format(
                                getResources().getString(R.string.toast_file_delete),
                                item.getName()
                        ),
                        Toast.LENGTH_SHORT
                ).show();
                profileManager.removeSavedRecording(item.getId());
                mFileViewerAdapter.notifyItemRemoved(position);
            }
        });
        mRecyclerView.setAdapter(mFileViewerAdapter);
        profileManager.getSavedRecordings(getActivity(), createOnSavedRecordingsChangedDataListener());
        return v;
    }

    private OnDataChangedListener<RecordingItem> createOnSavedRecordingsChangedDataListener() {

        return new OnDataChangedListener<RecordingItem>() {
            @Override
            public void onListChanged(List<RecordingItem> list) {
                progressBar.setVisibility(View.GONE);
                mFileViewerAdapter.setList(list);
            }
        };
    }
}




