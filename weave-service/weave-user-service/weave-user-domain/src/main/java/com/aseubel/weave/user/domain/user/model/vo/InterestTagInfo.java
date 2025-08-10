package com.aseubel.weave.user.domain.user.model.vo;

import lombok.Builder;
import lombok.Data;

/**
 * @author Aseubel
 * @date 2025/8/10 下午1:47
 */
@Data
@Builder
public class InterestTagInfo {
    private String name;
    private String color;
}
