package com.tju.elm_bk.adapter;

public interface MapAdapter {
    String navigationUrl(String address);
    default String navigationUrl(String address, com.tju.elm_bk.entity.GeoPoint point) { return navigationUrl(address); }

    String provider();
}
