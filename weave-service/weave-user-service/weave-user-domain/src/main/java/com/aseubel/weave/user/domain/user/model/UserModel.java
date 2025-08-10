package com.aseubel.weave.user.domain.user.model;

import com.aseubel.common.core.annotation.FieldDesc;
import com.aseubel.weave.user.domain.user.model.vo.CheckInDay;
import com.aseubel.weave.user.domain.user.model.vo.InterestTagInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * @author Aseubel
 * @date 2025/8/10 上午11:10
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserModel {
    @FieldDesc(name = "用户信息")
    private User user;

    @FieldDesc(name = "用户名")
    private String username;

    @FieldDesc(name = "昵称")
    private String nickname;

    @FieldDesc(name = "手机号码")
    private String mobile;

    @FieldDesc(name = "短信验证码")
    private String smsCode;

    @FieldDesc(name = "登录id")
    private String loginId;

    @FieldDesc(name = "密码")
    private String password;

    @FieldDesc(name = "旧密码")
    private String oldPassword;

    @FieldDesc(name = "新密码")
    private String newPassword;

    @FieldDesc(name = "确认密码")
    private String confirmPassword;

    @FieldDesc(name = "访问令牌")
    private String accessToken;

    @FieldDesc(name = "刷新令牌")
    private String refreshToken;

    @FieldDesc(name = "访问令牌过期时间（毫秒）")
    private Long accessTokenExpiration;

    @FieldDesc(name = "刷新令牌过期时间（毫秒）")
    private Long refreshTokenExpiration;

    private String avatarId;

    @FieldDesc(name = "角色名称集合")
    private Set<String> roles;

    @FieldDesc(name = "兴趣标签信息")
    private Set<InterestTagInfo> interestTagInfos;

    @FieldDesc(name = "签到信息")
    private CheckInStats checkInStats;

    private String ipAddress;
    private String deviceInfo;
    private Set<Long> interestTagIds;

    private Long id; // 签到记录ID
    private LocalDate checkDate; // 签到日期
    private LocalDateTime checkTime; // 签到时间
    private Integer pointsEarned; // 本次签到获得的积分
    private Integer consecutiveDays; // 连续签到天数
    private Boolean isMakeUp; // 是否为补签
    private Boolean todayChecked; // 当天是否已签到
    private Integer year; // 年份
    private Integer month; // 月份
    private List<CheckInDay> checkInDays; // 签到日期列表
    private Integer monthCheckDays; // 本月签到天数
    private Integer monthTotalDays; // 本月总天数

    public UserModel(User user) {
        this.user = user;
    }


}
