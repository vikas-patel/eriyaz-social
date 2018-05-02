package com.eriyaz.social.model;

/**
 * Created by vikas on 21/4/18.
 */

public class ReplyTextItem implements ListItem {
    String parentId;

    public ReplyTextItem(String parentId) {
        this.parentId = parentId;
    }

    @Override
    public int getListItemType() {
        return ListItem.EDIT_TEXT;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }
}
