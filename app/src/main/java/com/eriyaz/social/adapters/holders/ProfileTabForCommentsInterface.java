package com.eriyaz.social.adapters.holders;

import com.eriyaz.social.adapters.CommentsByUserAdapter;

public interface ProfileTabForCommentsInterface{
    void setCallBack(CommentsByUserAdapter.CallBack callBack);
    void loadFirstPage();
}
