package com.eriyaz.social.model;

public class RequestFeedback {
    private String feedbackerid;
    private String requesterid;
    private String feedbackid;
    private String postid;
    private long timestamp;
    public String message;

    public RequestFeedback(){

    }
    public RequestFeedback(String feedbackerid,String requesterid,String postid,String msg){
        this.feedbackerid=feedbackerid;
        this.requesterid=requesterid;
        this.postid=postid;
        message=msg;
    }

    public String getFeedbackerid(){
        return feedbackerid;
    }
    public String getRequesterid(){
        return requesterid;
    }
    public String getFeedbackid(){
        return feedbackid;
    }
    public String getPostid(){
        return postid;
    }
    public long getTimestamp(){
        return timestamp;
    }
    public void setFeedbackerid(String feedbackerid){
        this.feedbackid=feedbackerid;
    }
    public void setRequesterid(String requesterid){
        this.requesterid=requesterid;
    }
    public void setFeedbackid(String feedbackid){
        this.feedbackid=feedbackid;
    }
    public void setPostid(String postid){
        this.postid=postid;
    }
    public void setTimestamp(long timestamp){
        this.timestamp=timestamp;
    }
}
