package com.example.ngx.model;

public record DeviceInfo(
    String group,    // 设备分组（第3列）
    String deviceId, // 设备名（第5列）
    String password  // 设备密码（第6列）
) {}