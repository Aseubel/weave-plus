package com.aseubel.weave.user.trigger.controller.user;

import com.aseubel.common.core.ApiResponse;
import com.aseubel.common.core.PageResponse;
import com.aseubel.common.core.annotation.constraint.RequireLogin;
import com.aseubel.common.core.annotation.constraint.RequirePermission;
import com.aseubel.common.core.util.IpUtil;
import com.aseubel.weave.user.api.auth.request.ChangePasswordRequest;
import com.aseubel.weave.user.api.auth.response.LoginResponse;
import com.aseubel.weave.user.api.user.request.CheckInRequest;
import com.aseubel.weave.user.api.user.request.UserUpdateRequest;
import com.aseubel.weave.user.api.user.response.CheckInCalendarResponse;
import com.aseubel.weave.user.api.user.response.CheckInResponse;
import com.aseubel.weave.user.api.user.response.UserInfoResponse;
import com.aseubel.weave.user.domain.user.context.UserContext;
import com.aseubel.weave.user.domain.user.model.CheckInStats;
import com.aseubel.weave.user.domain.user.model.Image;
import com.aseubel.weave.user.domain.user.model.User;
import com.aseubel.weave.user.domain.user.model.UserModel;
import com.aseubel.weave.user.domain.user.model.vo.CheckInDay;
import com.aseubel.weave.user.domain.user.service.CheckInService;
import com.aseubel.weave.user.domain.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 用户控制器
 *
 * @author Aseubel
 * @date 2025/8/5 下午6:23
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final CheckInService checkInService;

    /**
     * 获取当前用户信息
     */
    @GetMapping("/info")
    @RequireLogin
    public ApiResponse<UserInfoResponse> getUserInfo() {
        UserModel model = userService.getUserInfo();
        UserInfoResponse userInfo = ofUserInfoResponse(model);
        return ApiResponse.success(userInfo);
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/info")
    @RequireLogin
    public ApiResponse<String> updateUserInfo(@Valid @RequestBody UserUpdateRequest request) {
        UserModel model = updateRequest2Model(request);
        userService.updateUserInfo(model);
        return ApiResponse.success("用户信息更新成功");
    }

    private UserModel updateRequest2Model(UserUpdateRequest request) {
        User user = User.builder()
                .nickname(request.getNickname())
                .mobile(request.getMobile())
                .bio(request.getBio())
                .birthday(request.getBirthday())
                .gender(request.getGender())
                .city(request.getCity())
                .profession(request.getProfession())
                .email(request.getEmail())
                .build();
        return UserModel.builder()
                .user(user)
                .avatarId(request.getAvatarId())
                .interestTagIds(request.getInterestTagIds())
                .build();
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    @RequireLogin
    public ApiResponse<String> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        UserModel model = pwRequest2Model(request);
        userService.changePassword(model);
        return ApiResponse.success("密码修改成功");
    }

    private UserModel pwRequest2Model(ChangePasswordRequest request) {
        return UserModel.builder()
                .oldPassword(request.getOldPassword())
                .newPassword(request.getNewPassword())
                .confirmPassword(request.getConfirmPassword())
                .build();
    }

    /**
     * 用户签到
     */
    @PostMapping("/check-in")
    @RequireLogin
    public ApiResponse<CheckInResponse> checkIn(
            @RequestBody(required = false) CheckInRequest request,
            HttpServletRequest httpRequest) {
        User currentUser = UserContext.getCurrentUser();

        String ipAddress = IpUtil.getClientIpAddress(httpRequest);
        String deviceInfo = IpUtil.getDeviceInfo(httpRequest);

        UserModel model = UserModel.builder()
                .user(currentUser)
                .ipAddress(ipAddress)
                .deviceInfo(deviceInfo)
                .build();
        model = checkInService.checkIn(model);

        CheckInResponse response = CheckInResponse.builder()
                .pointsEarned(model.getPointsEarned())
                .consecutiveDays(model.getConsecutiveDays())
                .checkTime(model.getCheckTime())
                .isMakeUp(false)
                .build();
        return ApiResponse.success(response);
    }

    /**
     * 获取签到日历
     */
    @GetMapping("/check-in/calendar")
    @RequireLogin
    public ApiResponse<CheckInCalendarResponse> getCheckInCalendar(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        User currentUser = UserContext.getCurrentUser();
        UserModel model = checkInService.getCheckInCalendar(currentUser, year, month);
        CheckInCalendarResponse calendar =
                CheckInCalendarResponse.builder()
                        .year(year)
                        .month(month)
                        .checkInDays(buildCheckInDay(model.getCheckInDays()))
                        .stats(buildStatsInfo(model.getCheckInStats()))
                        .build();
        return ApiResponse.success(calendar);
    }

    private List<CheckInCalendarResponse.CheckInDay> buildCheckInDay(List<CheckInDay> checkInDays) {
        return checkInDays.stream()
                .map(day -> CheckInCalendarResponse.CheckInDay.builder()
                        .date(day.getDate())
                        .checked(day.getChecked())
                        .isMakeUp(day.getIsMakeUp())
                        .pointsEarned(day.getPointsEarned())
                        .consecutiveDays(day.getConsecutiveDays())
                        .canMakeUp(day.getCanMakeUp())
                        .build())
                .toList();
    }

    /**
     * 获取签到统计
     */
    @GetMapping("/check-in/stats")
    @RequireLogin
    public ApiResponse<CheckInResponse.CheckInStats> getCheckInStats() {
        User currentUser = UserContext.getCurrentUser();
        CheckInResponse.CheckInStats stats = buildStatsInfo(checkInService.getCheckInStats(currentUser));
        return ApiResponse.success(stats);
    }

    /**
     * 检查今天是否已签到
     */
    @GetMapping("/check-in/today")
    @RequireLogin
    public ApiResponse<Boolean> isTodayChecked() {
        User currentUser = UserContext.getCurrentUser();
        boolean checked = checkInService.isTodayChecked(currentUser);
        return ApiResponse.success(checked);
    }

    /**
     * 获取用户列表（管理员功能）
     */
    @GetMapping("/list")
    @RequirePermission(roles = {"ADMIN"})
    public ApiResponse<PageResponse<UserInfoResponse>> getUserList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        PageResponse<UserModel> userList = userService.getUserList(page, size, keyword);
        PageResponse<UserInfoResponse> response = userList.transfer(this::ofUserInfoResponse, UserInfoResponse.class);
        return ApiResponse.success(response);
    }

    /**
     * 禁用/启用用户（管理员功能）
     */
    @PutMapping("/{userId}/status")
    @RequirePermission(roles = {"ADMIN"})
    public ApiResponse<String> toggleUserStatus(@PathVariable Long userId) {
        userService.toggleUserStatus(userId);
        return ApiResponse.success("用户状态更新成功");
    }

    private UserInfoResponse ofUserInfoResponse(UserModel userModel) {
        User user = userModel.getUser();
        return UserInfoResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .bio(user.getBio())
                .birthday(user.getBirthday())
                .gender(user.getGender())
                .city(user.getCity())
                .profession(user.getProfession())
                .email(user.getEmail())
                .mobile(user.getMobile())
                .avatar(user.getAvatar().getImageUrl())
                .points(user.getPoints())
                .level(user.getLevel())
                .createTime(user.getCreatedAt())
                .lastLoginTime(user.getLastLoginTime())
                .isActive(user.getIsActive())
                .build();
    }

    /**
     * 构建统计信息
     */
    private CheckInResponse.CheckInStats buildStatsInfo(CheckInStats stats) {
        return CheckInResponse.CheckInStats.builder()
                .totalCheckDays(stats.getTotalCheckDays())
                .currentConsecutiveDays(stats.getCurrentConsecutiveDays())
                .maxConsecutiveDays(stats.getMaxConsecutiveDays())
                .totalPointsEarned(stats.getTotalPointsEarned())
                .thisMonthCheckDays(stats.getThisMonthCheckDays())
                .availableMakeUp(stats.getAvailableMakeUp())
                .build();
    }
}
