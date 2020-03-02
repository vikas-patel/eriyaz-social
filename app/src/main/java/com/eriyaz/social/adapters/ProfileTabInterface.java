package com.eriyaz.social.adapters;

import com.eriyaz.social.adapters.holders.ProfileInterface;

/**
 * Created by vikas on 5/3/18.
 */

public interface ProfileTabInterface extends ProfileInterface {
    void setCallBack(PostsByUserAdapter.CallBack callBack);
}
