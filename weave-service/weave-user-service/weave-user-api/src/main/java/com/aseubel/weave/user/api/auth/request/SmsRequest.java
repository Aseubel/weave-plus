package com.aseubel.weave.user.api.auth.request;

import lombok.Getter;

import java.io.Serializable;

/**
 * @author Aseubel
 * @date 2025/6/30 下午8:27
 */
@Getter
public class SmsRequest implements Serializable {
    private String mobile;
}
