package com.aseubel.weave.user.domain.user.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * @author Aseubel
 * @date 2025/8/10 下午3:28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckInDay {
    private LocalDate date; // 日期
    private Boolean checked; // 是否已签到
    private Boolean isMakeUp; // 是否为补签
    private Integer pointsEarned; // 获得积分
    private Integer consecutiveDays; // 连续签到天数
    private Boolean canMakeUp; // 是否可以补签
}
