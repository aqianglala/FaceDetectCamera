package com.example.myfacedetectcamera.model;

public class UserModel {

    /**
     * userId : e160c5806b654dc8b0798bd7062a3ea4
     * userTime : 2018-11-27 00:00:00
     * userName : cj
     * authorize : 1
     * threshold : 0.45
     * personVoiceMessage : null
     * companyId : null
     * groupId : null
     * featureSize : 0
     */

    private String userId;
    private String userTime;
    private String userName;
    private int authorize;
    private double threshold;
    private Object personVoiceMessage;
    private Object companyId;
    private Object groupId;
    private int featureSize;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserTime() {
        return userTime;
    }

    public void setUserTime(String userTime) {
        this.userTime = userTime;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public int getAuthorize() {
        return authorize;
    }

    public void setAuthorize(int authorize) {
        this.authorize = authorize;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public Object getPersonVoiceMessage() {
        return personVoiceMessage;
    }

    public void setPersonVoiceMessage(Object personVoiceMessage) {
        this.personVoiceMessage = personVoiceMessage;
    }

    public Object getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Object companyId) {
        this.companyId = companyId;
    }

    public Object getGroupId() {
        return groupId;
    }

    public void setGroupId(Object groupId) {
        this.groupId = groupId;
    }

    public int getFeatureSize() {
        return featureSize;
    }

    public void setFeatureSize(int featureSize) {
        this.featureSize = featureSize;
    }
}
