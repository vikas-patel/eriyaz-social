package com.eriyaz.social.photomovie;

import android.app.Activity;

import com.eriyaz.social.photomovie.widget.FilterItem;
import com.eriyaz.social.photomovie.widget.TransferItem;
import com.hw.photomovie.render.GLTextureView;

import java.util.List;

/**
 * Created by huangwei on 2018/9/9.
 */
public interface IRecordShareView {
    public GLTextureView getGLView();
    void setFilters(List<FilterItem> filters);
    Activity getActivity();

    void setTransfers(List<TransferItem> items);
}
