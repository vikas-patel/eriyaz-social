package com.eriyaz.social.model;

import com.google.firebase.database.Exclude;

/**
 * Created by vikas on 21/4/18.
 */

public interface ListItem {
    int TEXT = 1;
    int TEXT_CHILD = 2;
    int EDIT_TEXT = 3;

    @Exclude
    int getListItemType();
}
