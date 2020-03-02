package com.eriyaz.social.model;

public class FeedbackRequestCount {
    private String feedbackRequestReceiverId;
    private Boolean hasAcceptedFeedbackRequest;

    public String getFeedbackRequestReceiverId() {
        return feedbackRequestReceiverId;
    }

    public Boolean getHasAcceptedFeedbackRequest() {
        return hasAcceptedFeedbackRequest;
    }

    public FeedbackRequestCount(String feedbackRequestReceiverId, Boolean hasAcceptedFeedbackRequest) {
        this.feedbackRequestReceiverId = feedbackRequestReceiverId;
        this.hasAcceptedFeedbackRequest = hasAcceptedFeedbackRequest;
    }

    public FeedbackRequestCount() {

    }
}
