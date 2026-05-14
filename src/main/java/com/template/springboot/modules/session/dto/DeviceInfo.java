package com.template.springboot.modules.session.dto;

public record DeviceInfo(String deviceName, String userAgent, String ipAddress) {

    public static DeviceInfo empty() {
        return new DeviceInfo(null, null, null);
    }
}
