package com.aseubel.weave.user.domain.user.service;

import com.aseubel.weave.user.domain.user.model.CheckInStats;
import com.aseubel.weave.user.domain.user.model.User;
import com.aseubel.weave.user.domain.user.model.UserModel;

/**
 * 签到服务接口
 * @author Aseubel
 * @date 2025/6/29
 */
public interface CheckInService {

    /**
     * 用户签到
     * @param model 用户模型
     * @return 签到响应
     */
    UserModel checkIn(UserModel model);

    /**
     * 获取用户签到日历
     * @param user 用户
     * @param year 年份
     * @param month 月份
     * @return 签到日历
     */
    UserModel getCheckInCalendar(User user, Integer year, Integer month);

    /**
     * 获取用户签到统计信息
     * @param user 用户
     * @return 签到响应（包含统计信息）
     */
    CheckInStats getCheckInStats(User user);

    /**
     * 检查用户今天是否已签到
     * @param user 用户
     * @return 是否已签到
     */
    boolean isTodayChecked(User user);

    /**
     * 计算签到积分
     * @param consecutiveDays 连续签到天数
     * @param isMakeUp 是否为补签
     * @return 积分
     */
    int calculateCheckInPoints(int consecutiveDays, boolean isMakeUp);

    /**
     * 初始化用户签到统计
     * @param user 用户
     */
    void initUserCheckInStats(User user);
}