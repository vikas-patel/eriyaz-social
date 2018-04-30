package com.eriyaz.social.adapters;

/**
 * Created by vikas on 5/3/18.
 */

public interface ProfileTabInterface {
    void loadPosts();
    void setCallBack(PostsByUserAdapter.CallBack callBack);
    void removeSelectedPost();
    void updateSelectedPost();
}