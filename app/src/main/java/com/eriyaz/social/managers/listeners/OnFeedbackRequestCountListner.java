package com.eriyaz.social.managers.listeners;

import com.eriyaz.social.model.FeedbackRequestCount;

import java.util.List;

public interface OnFeedbackRequestCountListner<FeedbackRequestCount> {
    public void onFeedbackRequestCountChanged(List<FeedbackRequestCount> list);
}
