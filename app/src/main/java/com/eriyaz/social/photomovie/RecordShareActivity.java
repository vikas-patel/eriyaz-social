package com.eriyaz.social.photomovie;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewStub;
import android.widget.Toast;

import com.eriyaz.social.ApplicationHelper;
import com.eriyaz.social.R;
import com.eriyaz.social.activities.BaseActivity;
import com.eriyaz.social.fragments.PlaybackFragment;
import com.eriyaz.social.managers.DatabaseHelper;
import com.eriyaz.social.model.RecordingItem;
import com.eriyaz.social.utils.LogUtil;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.hw.photomovie.model.PhotoData;
import com.hw.photomovie.render.GLTextureView;
import com.eriyaz.social.photomovie.widget.FilterItem;
import com.eriyaz.social.photomovie.widget.MovieBottomView;
import com.eriyaz.social.photomovie.widget.MovieFilterView;
import com.eriyaz.social.photomovie.widget.MovieTransferView;
import com.eriyaz.social.photomovie.widget.TransferItem;
import com.hw.photomovie.util.AppResources;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import me.iwf.photopicker.PhotoPicker;

/**
 * Created by huangwei on 2018/9/9.
 */
public class RecordShareActivity extends BaseActivity implements IRecordShareView, MovieBottomView.MovieBottomCallback {
    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }
    private static final int REQUEST_MUSIC = 234;
    public static final String MUSIC_URI_EXTRA_KEY = "RecordShareActivity.MUSIC_URI_EXTRA_KEY";
    public static final String POST_TITLE_EXTRA_KEY = "RecordShareActivity.POST_TITLE_EXTRA_KEY";
    public static final String PROFILE_URL_EXTRA_KEY = "RecordShareActivity.PROFILE_URL_EXTRA_KEY";
    public static final String PROFILE_NAME_EXTRA_KEY = "RecordShareActivity.PROFILE_NAME_EXTRA_KEY";
    public static final String AUDIO_DURATION_EXTRA_KEY = "RecordShareActivity.AUDIO_DURATION_EXTRA_KEY";

    private RecordSharePresenter mRecordSharePresenter;
    private GLTextureView mGLTextureView;
    private MovieFilterView mFilterView;
    private MovieTransferView mTransferView;
    private MovieBottomView mBottomView;
    private View mSelectView;
    private List<FilterItem> mFilters;
    private List<TransferItem> mTransfers;
    private View mFloatAddView;
    private String mMusicUrl;
    private String mProfileUrl;
    private String mProfileName;
    private String mPostTitle;
    private long mMusicDuration;
    private File localFile;
    public static final String TAG = RecordShareActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppResources.getInstance().init(getResources());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_share);

        mGLTextureView = findViewById(R.id.gl_texture);
        mBottomView = findViewById(R.id.movie_bottom_layout);
        mSelectView = findViewById(R.id.movie_add_text);
        mFloatAddView = findViewById(R.id.movie_add_float);

        mMusicUrl = getIntent().getStringExtra(MUSIC_URI_EXTRA_KEY);
        mMusicDuration = getIntent().getLongExtra(AUDIO_DURATION_EXTRA_KEY, 0);
        mProfileUrl = getIntent().getStringExtra(PROFILE_URL_EXTRA_KEY);
        mProfileName = getIntent().getStringExtra(PROFILE_NAME_EXTRA_KEY);
        mPostTitle = getIntent().getStringExtra(POST_TITLE_EXTRA_KEY);
        mRecordSharePresenter = new RecordSharePresenter(mMusicDuration, mProfileUrl, mPostTitle, mProfileName);
        mRecordSharePresenter.attachView(this);
        downloadAudioFile();
        mBottomView.setCallback(this);

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestPhotos();
            }
        };
        mSelectView.setOnClickListener(onClickListener);
        mFloatAddView.setOnClickListener(onClickListener);
        analytics.logFirstPost();
    }

    private void downloadAudioFile() {
        try {
            showProgress(R.string.progress_download_audio);
            localFile = File.createTempFile("RateMySinging", "mp4");
            DatabaseHelper databaseHelper = ApplicationHelper.getDatabaseHelper();
            databaseHelper.downloadAudio(localFile, mMusicUrl).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    // Local temp file has been created
                    Uri audioUri = Uri.fromFile(localFile);
                    mRecordSharePresenter.setMusic(audioUri);
                    ArrayList photos = new ArrayList();
                    photos.add(mProfileUrl);
                    mRecordSharePresenter.onPhotoPick(photos);
                    hideProgress();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    // Handle any errors
                    hideProgress();
                    showWarningDialog(e.getMessage());
//                    finish();
                }
            });

        } catch (IOException e) {
            showWarningDialog(e.getMessage());
//            finish();
        }
    }

    private void requestPhotos() {
        PhotoPicker.builder()
                .setPhotoCount(9)
                .setShowCamera(false)
                .setShowGif(false)
                .setPreviewEnabled(true)
                .start(this, PhotoPicker.REQUEST_CODE);
    }

    @Override
    public GLTextureView getGLView() {
        return mGLTextureView;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRecordSharePresenter.detachView();
        if (localFile != null) localFile.delete();
    }

    private boolean checkInit() {
        if (mSelectView.getVisibility() == View.VISIBLE) {
            Toast.makeText(this, "please select photos", Toast.LENGTH_LONG).show();
            return true;
        }
        return false;
    }

    @Override
    public void onNextClick() {
//        if (checkInit()) {
//            return;
//        }
        mRecordSharePresenter.saveVideo();
    }

    @Override
    public void onMusicClick() {
        if (checkInit()) {
            return;
        }
        Intent i = new Intent();
        i.setType("audio/*");
        i.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(i, REQUEST_MUSIC);
    }

    @Override
    public void onTransferClick() {
        if (checkInit()) {
            return;
        }
        if (mTransferView == null) {
            ViewStub stub = findViewById(R.id.movie_menu_transfer_stub);
            mTransferView = (MovieTransferView) stub.inflate();
            mTransferView.setVisibility(View.GONE);
            mTransferView.setItemList(mTransfers);
            mTransferView.setTransferCallback(mRecordSharePresenter);
        }
        mBottomView.setVisibility(View.GONE);
        mTransferView.show();
    }

    @Override
    public void onFilterClick() {
//        if (checkInit()) {
//            return;
//        }
        if (mFilterView == null) {
            ViewStub stub = findViewById(R.id.movie_menu_filter_stub);
            mFilterView = (MovieFilterView) stub.inflate();
            mFilterView.setVisibility(View.GONE);
            mFilterView.setItemList(mFilters);
            mFilterView.setFilterCallback(mRecordSharePresenter);
        }
        mBottomView.setVisibility(View.GONE);
        mFilterView.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == REQUEST_MUSIC) {
            Uri uri = data.getData();
            mRecordSharePresenter.setMusic(uri);
        } else if (resultCode == RESULT_OK && requestCode == PhotoPicker.REQUEST_CODE) {
            if (data != null) {
                ArrayList<String> photos = data.getStringArrayListExtra(PhotoPicker.KEY_SELECTED_PHOTOS);
                mRecordSharePresenter.onPhotoPick(photos);
                mFloatAddView.setVisibility(View.VISIBLE);
                mSelectView.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            if (mFilterView != null && mFilterView.getVisibility() == View.VISIBLE
                    && !checkInArea(mFilterView, ev)) {
                mFilterView.hide();
                mBottomView.setVisibility(View.VISIBLE);
                return true;
            } else if (mTransferView != null && mTransferView.getVisibility() == View.VISIBLE
                    && !checkInArea(mTransferView, ev)) {
                mTransferView.hide();
                mBottomView.setVisibility(View.VISIBLE);
                return true;
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private boolean checkInArea(View view, MotionEvent event) {
        int loc[] = new int[2];
        view.getLocationInWindow(loc);
        return event.getRawY() > loc[1];
    }

    @Override
    public void setFilters(List<FilterItem> filters) {
        mFilters = filters;
    }

    @Override
    public Activity getActivity() {
        return this;
    }

    @Override
    public void setTransfers(List<TransferItem> items) {
        mTransfers = items;
    }

    @Override
    public void onPause() {
        super.onPause();
        mRecordSharePresenter.onPause();
        mGLTextureView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mRecordSharePresenter.onResume();
        mGLTextureView.onResume();
    }
}
