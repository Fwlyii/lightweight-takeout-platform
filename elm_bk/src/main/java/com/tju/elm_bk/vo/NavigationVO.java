package com.tju.elm_bk.vo;

public record NavigationVO(
        Long taskId,
        String destinationType,
        String destinationName,
        String address,
        String navigationUrl,
        String provider
) {
}
