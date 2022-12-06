package com.hy.http.model;

import java.util.List;

public class Result {
    public List<DataItem> Data;

    public List<DataItem> getData() {
        return Data;
    }

    public void setData(List<DataItem> data) {
        Data = data;
    }
}
