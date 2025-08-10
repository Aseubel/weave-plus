package com.aseubel.weave.user.domain.user.service;

import com.aseubel.common.core.exception.BusinessException;
import com.aseubel.weave.user.domain.user.context.UserContext;
import com.aseubel.weave.user.domain.user.model.CheckIn;
import com.aseubel.weave.user.domain.user.model.CheckInStats;
import com.aseubel.weave.user.domain.user.model.User;
import com.aseubel.weave.user.domain.user.model.UserModel;
import com.aseubel.weave.user.domain.user.model.vo.CheckInDay;
import com.aseubel.weave.user.domain.user.repository.CheckInRepository;
import com.aseubel.weave.user.domain.user.repository.CheckInStatsRepository;
import com.aseubel.weave.user.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 签到服务实现类
 * 
 * @author Aseubel
 * @date 2025/6/29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckInServiceImpl implements CheckInService {

    private final CheckInRepository checkInRepository;
    private final CheckInStatsRepository checkInStatsRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserModel checkIn(UserModel model) {
        User user = UserContext.getCurrentUser();
        LocalDate today = LocalDate.now();

        // 检查今天是否已签到
        if (checkInRepository.existsByUserAndCheckDate(user, today)) {
            throw new BusinessException("今天已经签到过了");
        }

        // 获取或创建签到统计
        CheckInStats stats = getOrCreateCheckInStats(user);

        // 计算连续签到天数
        int consecutiveDays = calculateConsecutiveDays(user, today);

        // 计算积分
        int pointsEarned = calculateCheckInPoints(consecutiveDays, false);

        // 创建签到记录
        CheckIn checkIn = CheckIn.builder()
                .user(user)
                .checkDate(today)
                .checkTime(LocalDateTime.now())
                .pointsEarned(pointsEarned)
                .consecutiveDays(consecutiveDays)
                .ipAddress(model.getIpAddress())
                .deviceInfo(model.getDeviceInfo())
                .isMakeUp(false)
                .build();

        checkInRepository.save(checkIn);

        // 更新用户积分
        user.setPoints(user.getPoints() + pointsEarned);
        userRepository.save(user);

        // 更新签到统计
        updateCheckInStats(stats, checkIn, consecutiveDays);

        return UserModel.builder()
                .pointsEarned(pointsEarned)
                .consecutiveDays(consecutiveDays)
                .checkTime(checkIn.getCheckTime())
                .isMakeUp(false)
                .checkInStats(stats)
                .build();
    }

    @Override
    public UserModel getCheckInCalendar(User user, Integer year, Integer month) {
        if (year == null)
            year = LocalDate.now().getYear();
        if (month == null)
            month = LocalDate.now().getMonthValue();

        // 获取该月的签到记录
        List<CheckIn> checkIns = checkInRepository.findByUserAndYearAndMonth(user, year, month);
        Map<LocalDate, CheckIn> checkInMap = checkIns.stream()
                .collect(Collectors.toMap(CheckIn::getCheckDate, c -> c));

        // 获取签到统计
        CheckInStats stats = getOrCreateCheckInStats(user);

        // 构建日历数据
        YearMonth yearMonth = YearMonth.of(year, month);
        List<CheckInDay> checkInDays = new ArrayList<>();

        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            LocalDate date = LocalDate.of(year, month, day);
            CheckIn checkIn = checkInMap.get(date);

            boolean isChecked = checkIn != null;
            boolean canMakeUp = !isChecked &&
                    date.isBefore(LocalDate.now()) &&
                    date.isAfter(LocalDate.now().minusDays(7)) &&
                    stats.getAvailableMakeUp() > 0;

            CheckInDay checkInDay = CheckInDay.builder()
                    .date(date)
                    .checked(isChecked)
                    .isMakeUp(isChecked && checkIn.getIsMakeUp())
                    .pointsEarned(isChecked ? checkIn.getPointsEarned() : 0)
                    .consecutiveDays(isChecked ? checkIn.getConsecutiveDays() : 0)
                    .canMakeUp(canMakeUp)
                    .build();

            checkInDays.add(checkInDay);
        }

        return UserModel.builder()
                .year(year)
                .month(month)
                .checkInDays(checkInDays)
                .todayChecked(isTodayChecked(user))
                .checkInStats(CheckInStats.builder()
                        .totalCheckDays(stats.getTotalCheckDays())
                        .currentConsecutiveDays(stats.getCurrentConsecutiveDays())
                        .maxConsecutiveDays(stats.getMaxConsecutiveDays())
                        .thisMonthCheckDays(stats.getThisMonthCheckDays())
                        .availableMakeUp(stats.getAvailableMakeUp())
                        .build())
                .build();
    }

    @Override
    public CheckInStats getCheckInStats(User user) {
        return getOrCreateCheckInStats(user);
    }

    @Override
    public boolean isTodayChecked(User user) {
        return checkInRepository.existsByUserAndCheckDate(user, LocalDate.now());
    }

    @Override
    public int calculateCheckInPoints(int consecutiveDays, boolean isMakeUp) {
        int basePoints = 10; // 基础积分
        int bonusPoints = 0;

        // 连续签到奖励
        if (consecutiveDays >= 7) {
            bonusPoints += 20; // 连续7天奖励
        }
        if (consecutiveDays >= 30) {
            bonusPoints += 50; // 连续30天奖励
        }
        if (consecutiveDays >= 100) {
            bonusPoints += 100; // 连续100天奖励
        }

        int totalPoints = basePoints + bonusPoints;

        // 补签积分减半
        if (isMakeUp) {
            totalPoints = totalPoints / 2;
        }

        return Math.max(totalPoints, 1); // 最少1积分
    }

    @Override
    @Transactional
    public void initUserCheckInStats(User user) {
        if (!checkInStatsRepository.existsByUser(user)) {
            CheckInStats stats = CheckInStats.builder()
                    .user(user)
                    .build();
            checkInStatsRepository.save(stats);
        }
    }

    /**
     * 获取或创建签到统计
     */
    private CheckInStats getOrCreateCheckInStats(User user) {
        return checkInStatsRepository.findByUser(user)
                .orElseGet(() -> {
                    CheckInStats stats = CheckInStats.builder()
                            .user(user)
                            .build();
                    return checkInStatsRepository.save(stats);
                });
    }

    /**
     * 计算连续签到天数
     */
    private int calculateConsecutiveDays(User user, LocalDate checkDate) {
        List<CheckIn> recentCheckIns = checkInRepository.findConsecutiveCheckIns(user, checkDate.minusDays(1));

        int consecutiveDays = 1; // 包含今天
        LocalDate expectedDate = checkDate.minusDays(1);

        for (CheckIn checkIn : recentCheckIns) {
            if (checkIn.getCheckDate().equals(expectedDate)) {
                consecutiveDays++;
                expectedDate = expectedDate.minusDays(1);
            } else {
                break;
            }
        }

        return consecutiveDays;
    }

    /**
     * 更新签到统计
     */
    private void updateCheckInStats(CheckInStats stats, CheckIn checkIn, int consecutiveDays) {
        LocalDate today = LocalDate.now();

        // 更新基础统计
        stats.setTotalCheckDays(stats.getTotalCheckDays() + 1);
        stats.setCurrentConsecutiveDays(consecutiveDays);
        stats.setMaxConsecutiveDays(Math.max(stats.getMaxConsecutiveDays(), consecutiveDays));
        stats.setLastCheckDate(checkIn.getCheckDate());
        stats.setTotalPointsEarned(stats.getTotalPointsEarned() + checkIn.getPointsEarned());

        // 设置首次签到日期
        if (stats.getFirstCheckDate() == null) {
            stats.setFirstCheckDate(checkIn.getCheckDate());
        }

        // 更新月度统计
        if (checkIn.getCheckDate().getYear() == today.getYear() &&
                checkIn.getCheckDate().getMonth() == today.getMonth()) {
            stats.setThisMonthCheckDays(stats.getThisMonthCheckDays() + 1);
        }

        // 更新年度统计
        if (checkIn.getCheckDate().getYear() == today.getYear()) {
            stats.setThisYearCheckDays(stats.getThisYearCheckDays() + 1);
        }

        checkInStatsRepository.save(stats);
    }

}