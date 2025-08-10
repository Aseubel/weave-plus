package com.aseubel.weave.user.api.auth.request;

import lombok.Getter;

import java.io.Serializable;

/**
 * @author Aseubel
 * @date 2025/7/1 上午11:10
 */
@Getter
public class RefreshTokenRequest implements Serializable {

    private String refreshToken;
}
