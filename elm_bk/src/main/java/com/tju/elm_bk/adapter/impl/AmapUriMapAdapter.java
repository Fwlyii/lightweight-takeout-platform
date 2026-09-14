package com.tju.elm_bk.adapter.impl;

import com.tju.elm_bk.adapter.MapAdapter;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class AmapUriMapAdapter implements MapAdapter {
    @Override
    public String navigationUrl(String address) {
        return UriComponentsBuilder.fromUriString("https://uri.amap.com/search")
                .queryParam("keyword", address)
                .queryParam("viewMode", "map")
                .queryParam("callnative", "1")
                .build()
                .encode()
                .toUriString();
    }

    @Override
    public String provider() {
        return "AMAP_URI";
    }
}
