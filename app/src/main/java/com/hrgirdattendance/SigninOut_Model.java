package com.hrgirdattendance;

/**
 * Created by infogird47 on 08/07/2017.
 */

public class SigninOut_Model {
    String UserId,Date_Time, PrimaryKey, SignInOutId;

    public SigninOut_Model(String userId, String date_Time) {
        UserId = userId;
        Date_Time = date_Time;
    }

    public SigninOut_Model(String primary_key, String userId, String date_Time, String signInOutId) {
        PrimaryKey = primary_key;
        UserId = userId;
        Date_Time = date_Time;
        SignInOutId = signInOutId;
    }

    public SigninOut_Model() {
    }

    public String getPrimaryKey() {
        return PrimaryKey;
    }

    public String getUserId() {
        return UserId;
    }

    public void setUserId(String userId) {
        UserId = userId;
    }

    public void setPrimaryKey(String primaryKey) {
        PrimaryKey = primaryKey;
    }

    public String getDate_Time() {
        return Date_Time;
    }

    public void setDate_Time(String date_Time) {
        Date_Time = date_Time;
    }

    public String getSignInOutId() {
        return SignInOutId;
    }

    public void setSignInOutId(String signInOutId) {
        SignInOutId = signInOutId;
    }
}
