package com.hy.http.model;

import java.util.Date;

public class DataItem {
    public String UNIT;
    public String TAGDESC;
    public String tag;
    public Date time;
    public Double value;

    public String getUNIT() {
        return UNIT;
    }

    public void setUNIT(String UNIT) {
        this.UNIT = UNIT;
    }

    public String getTAGDESC() {
        return TAGDESC;
    }

    public void setTAGDESC(String TAGDESC) {
        this.TAGDESC = TAGDESC;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }
}
