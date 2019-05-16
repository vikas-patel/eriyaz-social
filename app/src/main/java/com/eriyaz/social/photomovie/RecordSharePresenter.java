package com.eriyaz.social.photomovie;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.widget.Toast;

import com.eriyaz.social.R;
import com.eriyaz.social.activities.BaseActivity;
import com.eriyaz.social.photomovie.widget.FilterItem;
import com.eriyaz.social.photomovie.widget.FilterType;
import com.eriyaz.social.photomovie.widget.MovieFilterView;
import com.eriyaz.social.photomovie.widget.MovieTransferView;
import com.eriyaz.social.photomovie.widget.TransferItem;
import com.eriyaz.social.utils.ImageUtil;
import com.eriyaz.social.utils.LogUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.ShortDynamicLink;
import com.hw.photomovie.PhotoMovie;
import com.hw.photomovie.PhotoMovieFactory;
import com.hw.photomovie.PhotoMoviePlayer;
import com.hw.photomovie.model.PhotoData;
import com.hw.photomovie.model.PhotoSource;
import com.hw.photomovie.model.SimplePhotoData;
import com.hw.photomovie.render.GLSurfaceMovieRenderer;
import com.hw.photomovie.render.GLTextureMovieRender;
import com.hw.photomovie.render.GLTextureView;
import com.hw.photomovie.segment.MovieSegment;
import com.hw.photomovie.timer.IMovieTimer;
import com.hw.photomovie.util.MLog;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import record.GLMovieRecorder;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by huangwei on 2018/9/9.
 */
public class RecordSharePresenter implements MovieFilterView.FilterCallback, IMovieTimer.MovieListener, MovieTransferView.TransferCallback {
    private static final String TAG = RecordSharePresenter.class.getSimpleName();
    private IRecordShareView mRecordShareView;

    private PhotoMovie mPhotoMovie;
    private PhotoMoviePlayer mPhotoMoviePlayer;
    private GLSurfaceMovieRenderer mMovieRenderer;
    private Uri mMusicUri;
    private long mMusicDuration = 0;
    private String mProfileUrl;
    private String mProfileName;
    private String mPostTitle;
    private PhotoMovieFactory.PhotoMovieType mMovieType = PhotoMovieFactory.PhotoMovieType.HORIZONTAL_TRANS;

    public RecordSharePresenter(long musicDuration, String profileUrl, String postTitle, String profileName) {
        this.mMusicDuration = musicDuration;
        this.mProfileUrl = profileUrl;
        this.mPostTitle = capitalizeFirstLetter(postTitle);
        this.mProfileName = capitalizeFirstLetter(profileName);
    }

    public String capitalizeFirstLetter(String original) {
        if (original == null || original.length() == 0) {
            return original;
        }
        return original.trim().substring(0, 1).toUpperCase() + original.substring(1);
    }

    public void attachView(IRecordShareView recordShareView) {
        mRecordShareView = recordShareView;
        initFilters();
        initTransfers();
        initMoviePlayer();
    }

    private void initTransfers() {
        List<TransferItem> items = new LinkedList<TransferItem>();
        items.add(new TransferItem(R.drawable.ic_movie_transfer, "LeftRight", PhotoMovieFactory.PhotoMovieType.HORIZONTAL_TRANS));
        items.add(new TransferItem(R.drawable.ic_movie_transfer, "UpDown", PhotoMovieFactory.PhotoMovieType.VERTICAL_TRANS));
        items.add(new TransferItem(R.drawable.ic_movie_transfer, "Window", PhotoMovieFactory.PhotoMovieType.WINDOW));
        items.add(new TransferItem(R.drawable.ic_movie_transfer, "Gradient", PhotoMovieFactory.PhotoMovieType.GRADIENT));
        items.add(new TransferItem(R.drawable.ic_movie_transfer, "Tranlation", PhotoMovieFactory.PhotoMovieType.SCALE_TRANS));
        items.add(new TransferItem(R.drawable.ic_movie_transfer, "Thaw", PhotoMovieFactory.PhotoMovieType.THAW));
        items.add(new TransferItem(R.drawable.ic_movie_transfer, "Scale", PhotoMovieFactory.PhotoMovieType.SCALE));
        mRecordShareView.setTransfers(items);
    }

    private void initFilters() {
        List<FilterItem> items = new LinkedList<FilterItem>();
        items.add(new FilterItem(R.drawable.filter_default, "None", FilterType.NONE));
        items.add(new FilterItem(R.drawable.gray, "BlackWhite", FilterType.GRAY));
        items.add(new FilterItem(R.drawable.kuwahara, "Watercolour", FilterType.KUWAHARA));
        items.add(new FilterItem(R.drawable.snow, "Snow", FilterType.SNOW));
        items.add(new FilterItem(R.drawable.l1, "Lut_1", FilterType.LUT1));
        items.add(new FilterItem(R.drawable.cameo, "Cameo", FilterType.CAMEO));
        items.add(new FilterItem(R.drawable.l2, "Lut_2", FilterType.LUT2));
        items.add(new FilterItem(R.drawable.l3, "Lut_3", FilterType.LUT3));
        items.add(new FilterItem(R.drawable.l4, "Lut_4", FilterType.LUT4));
        items.add(new FilterItem(R.drawable.l5, "Lut_5", FilterType.LUT5));
        mRecordShareView.setFilters(items);
    }

    private void initMoviePlayer() {
        final GLTextureView glTextureView = mRecordShareView.getGLView();

        mMovieRenderer = new GLTextureMovieRender(glTextureView);
        mPhotoMoviePlayer = new PhotoMoviePlayer(mRecordShareView.getActivity().getApplicationContext());
        mPhotoMoviePlayer.setMovieRenderer(mMovieRenderer);
        mPhotoMoviePlayer.setMovieListener(this);
        mPhotoMoviePlayer.setLoop(true);
        mPhotoMoviePlayer.setOnPreparedListener(new PhotoMoviePlayer.OnPreparedListener() {
            @Override
            public void onPreparing(PhotoMoviePlayer moviePlayer, float progress) {
            }

            @Override
            public void onPrepared(PhotoMoviePlayer moviePlayer, int prepared, int total) {
                mRecordShareView.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mPhotoMoviePlayer.start();
                    }
                });

            }

            @Override
            public void onError(PhotoMoviePlayer moviePlayer) {
                MLog.i("onPrepare", "onPrepare error");
            }
        });
    }

    private void startPlay(PhotoSource photoSource) {
        mPhotoMovie = generatePhotoMovie(photoSource, mMovieType);
        mPhotoMoviePlayer.setDataSource(mPhotoMovie);
        mPhotoMoviePlayer.prepare();
    }

    private PhotoMovie generatePhotoMovie(PhotoSource photoSource, PhotoMovieFactory.PhotoMovieType movieType) {
        int movieSegmentDutationMs = 0;
        int movieSegmentCount = photoSource.size();
        int fitCenterSegmentDurationMs =  ((int)mMusicDuration - (movieSegmentCount - 1)*movieSegmentDutationMs)/movieSegmentCount;
        List<MovieSegment> segmentList = new ArrayList<>();
        for (int i = 0; i < movieSegmentCount; i++) {
            segmentList.add(new FitCenterSubtitleSegment(fitCenterSegmentDurationMs).setBackgroundColor(0xFF323232));
//            segmentList.add(new MoveTransitionSegment(MoveTransitionSegment.DIRECTION_HORIZON, movieSegmentDutationMs));
        }
//        segmentList.remove(segmentList.size() - 1);
        PhotoMovie photoMovie = new PhotoMovie(photoSource, segmentList);
        return photoMovie;
    }

    public void detachView() {
        mRecordShareView = null;
    }

    @Override
    public void onFilterSelect(FilterItem item) {
        mMovieRenderer.setMovieFilter(item.initFilter());
    }

    @Override
    public void onMovieUpdate(int elapsedTime) {

    }

    @Override
    public void onMovieStarted() {

    }

    @Override
    public void onMoviedPaused() {

    }

    @Override
    public void onMovieResumed() {

    }

    @Override
    public void onMovieEnd() {

    }

    @Override
    public void onTransferSelect(TransferItem item) {
        mMovieType = item.type;
        mPhotoMoviePlayer.stop();
        mPhotoMovie = generatePhotoMovie(mPhotoMovie.getPhotoSource(), mMovieType);
        mPhotoMoviePlayer.setDataSource(mPhotoMovie);
        if (mMusicUri != null) {
            mPhotoMoviePlayer.setMusic(mRecordShareView.getActivity(), mMusicUri);
        }
        mPhotoMoviePlayer.setOnPreparedListener(new PhotoMoviePlayer.OnPreparedListener() {
            @Override
            public void onPreparing(PhotoMoviePlayer moviePlayer, float progress) {
            }

            @Override
            public void onPrepared(PhotoMoviePlayer moviePlayer, int prepared, int total) {
                mRecordShareView.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mPhotoMoviePlayer.start();
                    }
                });
            }

            @Override
            public void onError(PhotoMoviePlayer moviePlayer) {
                MLog.i("onPrepare", "onPrepare error");
            }
        });
        mPhotoMoviePlayer.prepare();
    }

    public void setMusic(Uri uri) {
        mMusicUri = uri;
        mPhotoMoviePlayer.setMusic(mRecordShareView.getActivity(), uri);
    }

    public void saveVideo() {
        if (Build.VERSION.SDK_INT < 18) {
            Toast.makeText(mRecordShareView.getActivity().getApplicationContext(), "Save video needs API 18 & above. Not supported on your phone!", Toast.LENGTH_LONG).show();
            return;
        }
        mPhotoMoviePlayer.pause();
        final ProgressDialog dialog = new ProgressDialog(mRecordShareView.getActivity());
        dialog.setMessage("preparing video...");
        dialog.setMax(100);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setCancelable(false);
        dialog.show();
        GLMovieRecorder recorder = new GLMovieRecorder();
        final File file = initVideoFile();
        GLTextureView glTextureView = mRecordShareView.getGLView();
//        int bitrate = glTextureView.getWidth() * glTextureView.getHeight() > 1000 * 1500 ? 8000000 : 4000000;
        int bitrate = 400000;
        recorder.configOutput(glTextureView.getWidth(), glTextureView.getHeight(), bitrate, 1, 1, file.getAbsolutePath());
        //生成一个全新的MovieRender，不然与现有的GL环境不一致，相互干扰容易出问题
        PhotoMovie newPhotoMovie = generatePhotoMovie(mPhotoMovie.getPhotoSource(), mMovieType);
        GLSurfaceMovieRenderer newMovieRenderer = new GLSurfaceMovieRenderer();
        newMovieRenderer.setMovieFilter(mMovieRenderer.getMovieFilter());
        newMovieRenderer.setPhotoMovie(newPhotoMovie);

        recorder.setDataSource(newMovieRenderer);
        long start0 = System.currentTimeMillis();

        recorder.startRecord(new GLMovieRecorder.OnRecordListener() {
            @Override
            public void onRecordFinish(boolean success) {
                long start1 = System.currentTimeMillis();
                LogUtil.logInfo(RecordShareActivity.TAG, "finished " + (start1 - start0));
                File outputFile = file;
                Toast.makeText(mRecordShareView.getActivity().getApplicationContext(), "Record finished:", Toast.LENGTH_LONG).show();
                if (mMusicUri != null) {
                        //合成音乐
                    File mixFile = initVideoFile();
//                        String audioPath = UriUtil.getPath(mRecordShareView.getActivity(), mMusicUri);
                    VideoUtil.addAudioVideo(file.getAbsolutePath(), mMusicUri.toString(), mixFile.getAbsolutePath());
                    file.delete();
                    outputFile = mixFile;
                }
                long start2 = System.currentTimeMillis();
                LogUtil.logInfo(RecordShareActivity.TAG, "audio mix " + (start2 - start1));
                dialog.dismiss();
                if (success) {
                    generateShortLink(outputFile);
                } else {
                    Toast.makeText(mRecordShareView.getActivity().getApplicationContext(), "record error!", Toast.LENGTH_LONG).show();
                }
                long start3 = System.currentTimeMillis();
                LogUtil.logInfo(RecordShareActivity.TAG, "share video" + (start3 - start2));
            }

            @Override
            public void onRecordProgress(int recordedDuration, int totalDuration) {
                dialog.setProgress((int) (recordedDuration / (float) totalDuration * 100));
            }
        });
    }


    //TODO: reuse DeepLinkUtil method
    private void generateShortLink(File outputFile) {
        FirebaseDynamicLinks.getInstance().createDynamicLink()
                .setLink(Uri.parse("https://play.google.com/store/apps/details?id=com.eriyaz.social&hl=en&invitedby=Ott9ue8YWBhLYpyXE5Vj2ZEKLGA3"))
                //.setDomainUriPrefix(mRecordShareView.getActivity().getString(R.string.dynamic_link_prefix))
                .setDynamicLinkDomain(mRecordShareView.getActivity().getString(R.string.dynamic_link_domain))
                //new dynamic url prefix
                // Set parameters
                // ...

                .buildShortDynamicLink()
                .addOnCompleteListener(mRecordShareView.getActivity(), new OnCompleteListener<ShortDynamicLink>() {
                    @Override
                    public void onComplete(@NonNull Task<ShortDynamicLink> task) {
                        if (task.isSuccessful()) {

                            Toast.makeText(mRecordShareView.getActivity().getApplicationContext(), "Video save to path:" + outputFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
                            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                            sharingIntent.setType("video/mp4");
                            Uri uri = FileProvider.getUriForFile(mRecordShareView.getActivity(),
                                    mRecordShareView.getActivity().getString(R.string.file_provider_authority),
                                    outputFile);
                            sharingIntent.putExtra(Intent.EXTRA_STREAM, uri);

                            String shortLink=task.getResult().getShortLink().toString();
                            sharingIntent.putExtra(Intent.EXTRA_TEXT, mRecordShareView.getActivity().getString(R.string.post_share_description, mProfileName, mPostTitle)+mRecordShareView.getActivity().getString(R.string.joinapp)+" " +shortLink);
                            mRecordShareView.getActivity().startActivity(Intent.createChooser(sharingIntent, "Share Video!"));
                            BaseActivity activity = (BaseActivity) mRecordShareView.getActivity();
                            activity.getAnalytics().logShareVideo();


                        } else {
                            // Error
                            // ...
                            Log.e(TAG,task.getException().getMessage(), task.getException());
                        }
                    }
                });
    }




    private File initVideoFile() {
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        if (!dir.exists()) {
            dir = mRecordShareView.getActivity().getCacheDir();
        }
        String fileName = mPostTitle + "-" + new Date().getTime() + ".mp4";
        return new File(dir, fileName);
    }

    public void onPause() {
        mPhotoMoviePlayer.pause();
    }

    public void onResume() {
        mPhotoMoviePlayer.start();
    }

    public void onPhotoPick(ArrayList<String> photos) {
        List<PhotoData> photoDataList = new ArrayList<PhotoData>(photos.size());
        for (String path : photos) {
            PhotoData photoData;
            if (path == null || path.isEmpty()) {
                Drawable drawable = ImageUtil.getTextDrawable(mProfileName, 300, 300);
                Bitmap bitmap = ImageUtil.drawableToBitmap(drawable);
                photoData = new SimplePhotoData(mRecordShareView.getActivity(), path, PhotoData.STATE_BITMAP);
                photoData.setBitmap(bitmap);
            } else {
                photoData = new SimplePhotoData(mRecordShareView.getActivity(), path, PhotoData.STATE_LOCAL);
            }
            photoDataList.add(photoData);
        }
        PhotoSource photoSource = new PhotoSource(photoDataList);
        if (mPhotoMoviePlayer == null) {
            startPlay(photoSource);
        } else {
            mPhotoMoviePlayer.stop();
            mPhotoMovie = generatePhotoMovie(photoSource, PhotoMovieFactory.PhotoMovieType.HORIZONTAL_TRANS);
            mPhotoMoviePlayer.setDataSource(mPhotoMovie);
            if (mMusicUri != null) {
                mPhotoMoviePlayer.setMusic(mRecordShareView.getActivity(), mMusicUri);
            }
            mPhotoMoviePlayer.setOnPreparedListener(new PhotoMoviePlayer.OnPreparedListener() {
                @Override
                public void onPreparing(PhotoMoviePlayer moviePlayer, float progress) {
                }

                @Override
                public void onPrepared(PhotoMoviePlayer moviePlayer, int prepared, int total) {
                    if (mRecordShareView.getActivity() == null) {
                        // activity has destroyed
                        return;
                    }
                    mRecordShareView.getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mPhotoMoviePlayer.start();
                        }
                    });
                }

                @Override
                public void onError(PhotoMoviePlayer moviePlayer) {
                    MLog.i("onPrepare", "onPrepare error");
                }
            });
            mPhotoMoviePlayer.prepare();
        }
    }
}
