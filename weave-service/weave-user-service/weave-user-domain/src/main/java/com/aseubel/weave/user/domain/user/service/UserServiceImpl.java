package com.aseubel.weave.user.domain.user.service;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.aseubel.common.core.PageResponse;
import com.aseubel.common.core.exception.AuthenticationException;
import com.aseubel.common.core.exception.BusinessException;
import com.aseubel.common.core.util.JwtUtil;
import com.aseubel.common.redis.service.IRedisService;
import com.aseubel.weave.user.common.redis.KeyBuilder;
import com.aseubel.weave.user.domain.user.context.UserContext;
import com.aseubel.weave.user.domain.user.model.*;
import com.aseubel.weave.user.domain.user.model.vo.InterestTagInfo;
import com.aseubel.weave.user.domain.user.repository.CheckInRepository;
import com.aseubel.weave.user.domain.user.repository.CheckInStatsRepository;
import com.aseubel.weave.user.domain.user.repository.InterestTagRepository;
import com.aseubel.weave.user.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.aseubel.common.core.Constant.SMS_CODE_EXPIRE_MINUTES;

/**
 * 用户服务实现类
 *
 * @author Aseubel
 * @date 2025/6/27
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final CheckInRepository checkInRepository;
    private final CheckInStatsRepository checkInStatsRepository;
    private final InterestTagRepository interestTagRepository;
    private final JwtUtil jwtUtil;
    private final IRedisService redissonService;
    //    private final WebClient.Builder webClientBuilder;
    private final ThreadPoolTaskExecutor threadPoolExecutor;

    @Value("${jwt.access-token-expiration}")
    private Long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;

    @Value("${third-party.wechat.app-id}")
    private String wechatAppId;

    @Value("${third-party.wechat.app-secret}")
    private String wechatAppSecret;

    @Value("${third-party.qq.app-id}")
    private String qqAppId;

    @Value("${third-party.qq.app-key}")
    private String qqAppKey;

    @Value("${app.password}")
    private String defaultPassword;

    @Override
    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public UserModel login(UserModel model) {
        // 查找用户
        User user = userRepository.findByUsernameOrMobile(model.getLoginId())
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));

        // 验证密码
        if (!BCrypt.checkpw(model.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }
        model.setUser(user);
        updateLastLoginTime(user);
        return generateToken(model);
    }

    @Override
    @Transactional
    public UserModel mobileLogin(UserModel model) {
        // 验证短信验证码
        if (!verifySmsCode(model.getLoginId(), model.getSmsCode())) {
            throw new RuntimeException("验证码错误或已过期");
        }

        // 查找或创建用户
        User user = userRepository.findByMobile(model.getLoginId())
                .orElseGet(() -> {
                    // 如果用户不存在，自动注册
                    User newUser = User.builder()
                            .mobile(model.getLoginId())
                            .password(BCrypt.hashpw(defaultPassword, BCrypt.gensalt()))
                            .nickname("用户" + model.getLoginId().substring(7))
                            .username("mobile_" + model.getLoginId())
                            .build();
                    return userRepository.save(newUser);
                });

        updateLastLoginTime(user);
        model.setUser(user);

        return generateToken(model);
    }

//    @Override
//    @Transactional
//    public LoginResponse thirdPartyLogin(ThirdPartyLoginRequest request) {
//        String openId;
//        String nickname;
//        String avatarUrl;
//        User user = null;
//
//        if (WECHAT.equals(request.getType())) {
//            // 微信登录逻辑
//            WechatUserInfo wechatUserInfo = getWechatUserInfo(request.getCode());
//            openId = wechatUserInfo.getOpenid();
//            nickname = wechatUserInfo.getNickname();
//            avatarUrl = wechatUserInfo.getHeadimgurl();
//
//            // 查找或创建用户
//            user = userRepository.findByWechatOpenId(openId)
//                    .orElseGet(() -> {
//                        User newUser = User.builder()
//                                .wechatOpenId(openId)
//                                .nickname(nickname)
//                                .avatar(Image.builder().imageUrl(avatarUrl).build())
//                                .username("wechat_" + openId.substring(0, 8))
//                                .build();
//                        return userRepository.save(newUser);
//                    });
//        } else if (QQ.equals(request.getType())) {
//            // QQ登录逻辑
//            QQUserInfo qqUserInfo = getQQUserInfo(request.getCode());
//            openId = qqUserInfo.getOpenid();
//            nickname = qqUserInfo.getNickname();
//            avatarUrl = qqUserInfo.getFigureurl_qq_1();
//
//            // 查找或创建用户
//            user = userRepository.findByQqOpenId(openId)
//                    .orElseGet(() -> {
//                        User newUser = User.builder()
//                                .qqOpenId(openId)
//                                .nickname(nickname)
//                                .avatar(Image.builder().imageUrl(avatarUrl).build())
//                                .username("qq_" + openId.substring(0, 8))
//                                .build();
//                        return userRepository.save(newUser);
//                    });
//        } else {
//            throw new RuntimeException("不支持的第三方平台");
//        }
//        updateLastLoginTime(user);
//        return generateTokenResponse(user);
//    }

    @Override
    @Transactional
    public UserModel register(UserModel model) {
        // 验证密码确认
        if (!model.getPassword().equals(model.getConfirmPassword())) {
            throw new BusinessException(400, "两次输入的密码不一致");
        }

        // 检查用户名是否存在
        if (existsByUsername(model.getUsername())) {
            throw new BusinessException(400, "用户名已存在");
        }

        // 检查手机号是否存在（如果提供了手机号）
        if (StringUtils.hasText(model.getMobile())) {
            if (existsByMobile(model.getMobile())) {
                throw new BusinessException(400, "手机号已被注册");
            }
            // 验证短信验证码
            if (!verifySmsCode(model.getMobile(), model.getSmsCode())) {
                throw new BusinessException(400, "验证码错误或已过期");
            }
        }

        User user = User.builder()
                .username(model.getUsername())
                .password(BCrypt.hashpw(model.getPassword(), BCrypt.gensalt()))
                .nickname(model.getNickname())
                .mobile(model.getMobile())
                .build();

        model.setUser(userRepository.save(user));
        return generateToken(model);
    }

    @Override
    public UserModel refreshToken(String refreshToken) {
        // 验证刷新令牌
        if (!jwtUtil.validateToken(refreshToken) || !jwtUtil.isRefreshToken(refreshToken)) {
            throw new AuthenticationException("刷新令牌无效或已过期");
        }

        Long userId = jwtUtil.getUserIdFromToken(refreshToken);
        User user = findById(userId);
        if (user == null) {
            throw new BusinessException(400, "用户不存在");
        }

        return generateToken(new UserModel(user));
    }

    @Override
    public void logout(Long userId) {
        // 从Redis中删除token
        String redisKey = KeyBuilder.userTokenKey(userId);
        redissonService.remove(redisKey);
    }

    @Override
    public String sendSmsCode(String mobile) {
        // 生成6位数字验证码
        String code = RandomUtil.randomNumbers(6);

        // 存储到Redis，5分钟过期
        String redisKey = KeyBuilder.smsCodeKey(mobile);
        redissonService.setValue(redisKey, code, Duration.ofMinutes(5).toMillis());

        // 这里应该调用短信服务发送验证码
        // 为了演示，我们只是记录日志
        log.info("发送短信验证码到 {}: {}", mobile, code);
        return code;

        // TODO 实际项目中应该调用阿里云、腾讯云等短信服务
        // sendSmsViaSmsProvider(mobile, code, type);
    }

    @Override
    public boolean verifySmsCode(String mobile, String code) {
        String redisKey = KeyBuilder.smsCodeKey(mobile);
        String storedCode = redissonService.getValue(redisKey);

        if (storedCode != null && storedCode.equals(code)) {
            // 验证成功后删除验证码
            redissonService.remove(redisKey);
            return true;
        }
        return false;
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByMobile(String mobile) {
        return userRepository.existsByMobile(mobile);
    }

    /**
     * 生成token响应
     */
    private UserModel generateToken(UserModel model) {
        User user = model.getUser();
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());

        // 将访问令牌存储到Redis
        String redisKey = KeyBuilder.userTokenKey(user.getId());
        redissonService.setValue(redisKey, accessToken, accessTokenExpiration);

        model.setAccessToken(accessToken);
        model.setAccessTokenExpiration(System.currentTimeMillis() + accessTokenExpiration);
        model.setRefreshToken(refreshToken);
        model.setRefreshTokenExpiration(System.currentTimeMillis() + refreshTokenExpiration);

        return model;
    }

    /**
     * 获取微信用户信息
     */
    private WechatUserInfo getWechatUserInfo(String code) {
        // 1. 通过code获取access_token
        String tokenUrl = String.format(
                "https://api.weixin.qq.com/sns/oauth2/access_token?appid=%s&secret=%s&code=%s&grant_type=authorization_code",
                wechatAppId, wechatAppSecret, code);

        // 这里应该实际调用微信API
        // 为了演示，返回模拟数据
        WechatUserInfo userInfo = new WechatUserInfo();
        userInfo.setOpenid("mock_wechat_openid_" + code.substring(0, 8));
        userInfo.setNickname("微信用户");
        userInfo.setHeadimgurl("https://example.com/avatar.jpg");

        return userInfo;
    }

    /**
     * 获取QQ用户信息
     */
    private QQUserInfo getQQUserInfo(String code) {
        // 1. 通过code获取access_token
        // 2. 通过access_token获取openid
        // 3. 通过access_token和openid获取用户信息

        // 这里应该实际调用QQ API
        // 为了演示，返回模拟数据
        QQUserInfo userInfo = new QQUserInfo();
        userInfo.setOpenid("mock_qq_openid_" + code.substring(0, 8));
        userInfo.setNickname("QQ用户");
        userInfo.setFigureurl_qq_1("https://example.com/avatar.jpg");

        return userInfo;
    }

    // 微信用户信息DTO
    private static class WechatUserInfo {
        private String openid;
        private String nickname;
        private String headimgurl;

        // getters and setters
        public String getOpenid() {
            return openid;
        }

        public void setOpenid(String openid) {
            this.openid = openid;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public String getHeadimgurl() {
            return headimgurl;
        }

        public void setHeadimgurl(String headimgurl) {
            this.headimgurl = headimgurl;
        }
    }

    // QQ用户信息DTO
    private static class QQUserInfo {
        private String openid;
        private String nickname;
        private String figureurl_qq_1;

        // getters and setters
        public String getOpenid() {
            return openid;
        }

        public void setOpenid(String openid) {
            this.openid = openid;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public String getFigureurl_qq_1() {
            return figureurl_qq_1;
        }

        public void setFigureurl_qq_1(String figureurl_qq_1) {
            this.figureurl_qq_1 = figureurl_qq_1;
        }
    }

    @Override
    public UserModel getUserInfo() {
        User user = UserContext.getCurrentUser();
        return getUserInfo(user);
    }

    private UserModel getUserInfo(User user) {
        // 获取签到统计信息
        CheckInStats checkInStats = checkInStatsRepository.findByUser(user).orElse(null);

        // 构建角色信息
        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        // 构建兴趣标签信息
        Set<InterestTagInfo> interestTagInfos = user.getInterestTags().stream()
                .map(tag -> InterestTagInfo.builder()
                        .name(tag.getName())
                        .color(tag.getColor())
                        .build())
                .collect(Collectors.toSet());

        return UserModel.builder()
                .user(user)
                .roles(roleNames)
                .checkInStats(checkInStats)
                .interestTagInfos(interestTagInfos)
                .build();
    }

    @Override
    @Transactional
    public void updateUserInfo(UserModel model) {
        User user = UserContext.getCurrentUser();
        User targetUser = model.getUser();
        // 更新基本信息
        if (StringUtils.hasText(targetUser.getNickname())) {
            user.setNickname(targetUser.getNickname());
        }
        if (StringUtils.hasText(targetUser.getBio())) {
            user.setBio(targetUser.getBio());
        }
        if (targetUser.getBirthday() != null) {
            user.setBirthday(targetUser.getBirthday());
        }
        if (StringUtils.hasText(targetUser.getGender())) {
            user.setGender(targetUser.getGender());
        }
        if (StringUtils.hasText(targetUser.getCity())) {
            user.setCity(targetUser.getCity());
        }
        if (StringUtils.hasText(targetUser.getProfession())) {
            user.setProfession(targetUser.getProfession());
        }
        if (StringUtils.hasText(targetUser.getEmail())) {
            user.setEmail(targetUser.getEmail());
        }
        if (StringUtils.hasText(targetUser.getMobile())) {
            // 检查手机号是否已被其他用户使用
            if (!targetUser.getMobile().equals(user.getMobile()) &&
                    userRepository.existsByMobile(targetUser.getMobile())) {
                throw new BusinessException("手机号已被使用");
            }
            user.setMobile(targetUser.getMobile());
        }
        if (ObjectUtil.isNotEmpty(model.getAvatarId())) {
            user.setAvatar(Image.builder().id(model.getAvatarId()).build());
        }

        // 更新兴趣标签
        if (model.getInterestTagIds() != null) {
            Set<InterestTag> interestTags = new HashSet<>(interestTagRepository.findAllById(model.getInterestTagIds()));
            user.setInterestTags(interestTags);
        }

        userRepository.save(user);
    }

    @Override
    @Transactional
    public void changePassword(UserModel model) {
        User user = UserContext.getCurrentUser();
        // 验证确认密码
        if (!model.getNewPassword().equals(model.getConfirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }

        // 验证原密码
        if (!BCrypt.checkpw(model.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码错误");
        }

        // 检查新密码是否与原密码相同
        if (BCrypt.checkpw(model.getNewPassword(), user.getPassword())) {
            throw new BusinessException("新密码不能与原密码相同");
        }

        // 更新密码
        user.setPassword(BCrypt.hashpw(model.getNewPassword(), BCrypt.gensalt()));
        userRepository.save(user);

        // 清除用户的所有token，强制重新登录
        String tokenKey = KeyBuilder.userTokenKey(user.getId());
        redissonService.remove(tokenKey);

        log.info("用户 {} 修改密码成功", user.getUsername());
    }

    @Override
    public PageResponse<UserModel> getUserList(int page, int size, String keyword) {
        Sort.TypedSort<User> sortType = Sort.sort(User.class);
        Pageable pageable = PageRequest.of(page - 1, size, sortType.by(User::getCreatedAt).descending());

        Page<User> userPage;
        if (StringUtils.hasText(keyword)) {
            userPage = userRepository.findByUsernameContainingOrNicknameContainingOrMobileContaining(
                    keyword, keyword, keyword, pageable);
        } else {
            userPage = userRepository.findAll(pageable);
        }

        List<UserModel> userInfoList = userPage.getContent().stream()
                .map(this::getUserInfo)
                .collect(Collectors.toList());

        return PageResponse.of(userInfoList, page, size, userPage.getTotalElements());
    }

    @Override
    @Transactional
    public void toggleUserStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        user.setIsActive(!user.getIsActive());
        userRepository.save(user);

        // 如果禁用用户，清除其token
        if (!user.getIsActive()) {
            String tokenKey = KeyBuilder.userTokenKey(userId);
            redissonService.remove(tokenKey);
        }

        log.info("用户 {} 状态已更新为: {}", user.getUsername(), user.getIsActive() ? "启用" : "禁用");
    }

    private void updateLastLoginTime(User user) {
        user.setLastLoginTime(LocalDateTime.now());
        userRepository.save(user);
    }
}