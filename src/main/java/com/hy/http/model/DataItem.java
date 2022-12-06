package com.hy.http.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

public class DataItem {
    public String TagName;
    public String ClientHost;
    public String Confidence;
    public Double Value;
    public Date TimeStamp;
    public String HostName;
    public String Units;
    public String Tolerance;

    public String getTagName() {
        return TagName;
    }

    public void setTagName(String tagName) {
        TagName = tagName;
    }

    public String getClientHost() {
        return ClientHost;
    }

    public void setClientHost(String clientHost) {
        ClientHost = clientHost;
    }

    public String getConfidence() {
        return Confidence;
    }

    public void setConfidence(String confidence) {
        Confidence = confidence;
    }

    public Double getValue() {
        return Value;
    }

    public void setValue(Double value) {
        Value = value;
    }

    public Date getTimeStamp() {
        return TimeStamp;
    }

    public void setTimeStamp(Date timeStamp) {
        TimeStamp = timeStamp;
    }

    public String getHostName() {
        return HostName;
    }

    public void setHostName(String hostName) {
        HostName = hostName;
    }

    public String getUnits() {
        return Units;
    }

    public void setUnits(String units) {
        Units = units;
    }

    public String getTolerance() {
        return Tolerance;
    }

    public void setTolerance(String tolerance) {
        Tolerance = tolerance;
    }
}
