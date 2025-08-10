package com.aseubel.weave.user.domain.user.service;

import com.aliyuncs.ram.model.v20150501.ChangePasswordRequest;
import com.aseubel.common.core.PageResponse;
import com.aseubel.weave.user.domain.user.model.User;
import com.aseubel.weave.user.domain.user.model.UserModel;

/**
 * @author Aseubel
 * @date 2025/8/5 上午10:17
 */
public interface UserService {

    /**
     * 根据ID查找用户
     */
    User findById(Long id);

    /**
     * 用户名密码登录
     */
    UserModel login(UserModel request);

    /**
     * 手机验证码登录
     */
    UserModel mobileLogin(UserModel request);

//    /**
//     * 第三方登录
//     */
//    UserModel thirdPartyLogin(ThirdPartyLoginRequest request);

    /**
     * 用户注册
     */
    UserModel register(UserModel request);

    /**
     * 刷新token
     */
    UserModel refreshToken(String refreshToken);

    /**
     * 用户登出
     */
    void logout(Long userId);

    /**
     * 发送短信验证码
     */
    String sendSmsCode(String mobile);

    /**
     * 验证短信验证码
     */
    boolean verifySmsCode(String mobile, String code);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 检查手机号是否存在
     */
    boolean existsByMobile(String mobile);

    /**
     * 获取用户信息
     */
    UserModel getUserInfo();

    /**
     * 更新用户信息
     */
    void updateUserInfo(UserModel request);

    /**
     * 修改密码
     */
    void changePassword(UserModel model);

    /**
     * 获取用户列表（分页）
     */
    PageResponse<UserModel> getUserList(int page, int size, String keyword);

    /**
     * 切换用户状态（启用/禁用）
     */
    void toggleUserStatus(Long userId);
}
