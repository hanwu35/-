package com.example.aihealthcheck.service;

import com.example.aihealthcheck.entity.User;
import com.example.aihealthcheck.repository.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    // ============ 基础 CRUD 方法 ============

    /**
     * 通过 ID 查找用户
     */
    public Optional<User> findById(Integer id) {
        return userRepository.findById(id);
    }

    /**
     * 通过 ID 获取用户（别名方法）- UserController 需要
     */
    public Optional<User> getUserById(Integer id) {
        return userRepository.findById(id);
    }

    /**
     * 获取所有用户
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * 保存用户
     */
    public User save(User user) {
        return userRepository.save(user);
    }

    /**
     * 删除用户
     */
    public void delete(Integer id) {
        userRepository.deleteById(id);
    }

    /**
     * 统计用户总数 - UserController 需要
     */
    public long count() {
        return userRepository.count();
    }

    // ============ 业务方法 ============

    /**
     * 用户注册
     */
    public User registerUser(String account, String password, String realName,
                           String gender, Integer age, String category) {
        // 检查账号是否已存在
        if (userRepository.existsByAccount(account)) {
            throw new RuntimeException("账号已存在");
        }

        // 创建用户
        User user = new User(account, password, realName, gender, age, category);
        return userRepository.save(user);
    }

    /**
     * 用户登录验证
     */
    public Optional<User> login(String account, String password) {
        return userRepository.findByAccountAndPassword(account, password);
    }

    /**
     * 通过账号查找用户
     */
    public Optional<User> findByAccount(String account) {
        return userRepository.findByAccount(account);
    }

    /**
     * 更新用户信息
     */
    public User updateUser(Integer id, User userDetails) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 更新基本信息
        user.setRealName(userDetails.getRealName());
        user.setGender(userDetails.getGender());
        user.setAge(userDetails.getAge());
        user.setCategory(userDetails.getCategory());

        // 如果提供了新密码，更新密码
        if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
            user.setPassword(userDetails.getPassword());
        }

        // 如果需要更新账号，先检查唯一性
        if (userDetails.getAccount() != null &&
            !userDetails.getAccount().isEmpty() &&
            !userDetails.getAccount().equals(user.getAccount())) {

            if (userRepository.existsByAccount(userDetails.getAccount())) {
                throw new RuntimeException("账号已存在");
            }
            user.setAccount(userDetails.getAccount());
        }

        return userRepository.save(user);
    }

    /**
     * 删除用户
     */
    public void deleteUser(Integer id) {
        userRepository.deleteById(id);
    }

    // ============ 查询方法 - UserController 需要 ============

    /**
     * 根据类型获取用户 - UserController 需要
     */
    public List<User> getUsersByCategory(String category) {
        return userRepository.findAll().stream()
                .filter(user -> user.getCategory().equals(category))
                .toList();
    }

    /**
     * 根据类别统计用户数量 - UserController 需要
     */
    public long countUsersByCategory(String category) {
        return userRepository.findAll().stream()
                .filter(user -> user.getCategory().equals(category))
                .count();
    }

    /**
     * 检查账号是否存在
     */
    public boolean existsByAccount(String account) {
        return userRepository.existsByAccount(account);
    }

    /**
     * 搜索用户（根据姓名或账号）- UserController 需要
     */
    public List<User> searchUsers(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllUsers();
        }

        String searchKeyword = keyword.trim().toLowerCase();
        return userRepository.findAll().stream()
                .filter(user ->
                    (user.getRealName() != null && user.getRealName().toLowerCase().contains(searchKeyword)) ||
                    (user.getAccount() != null && user.getAccount().toLowerCase().contains(searchKeyword)))
                .toList();
    }

    // ============ 辅助方法 ============

    /**
     * 创建用户（简化方法）- UserController 需要
     */
    public User createUser(User user) {
        if (user == null) {
            throw new RuntimeException("用户信息不能为空");
        }

        // 检查账号是否已存在
        if (userRepository.existsByAccount(user.getAccount())) {
            throw new RuntimeException("账号已存在");
        }

        return userRepository.save(user);
    }

    /**
     * 验证用户凭据
     */
    public boolean validateCredentials(String account, String password) {
        return userRepository.findByAccountAndPassword(account, password).isPresent();
    }

    /**
     * 更改密码
     */
    public boolean changePassword(Integer id, String oldPassword, String newPassword) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();
        // 验证旧密码
        if (!user.getPassword().equals(oldPassword)) {
            return false;
        }

        // 更新密码
        user.setPassword(newPassword);
        userRepository.save(user);
        return true;
    }

    /**
     * 重置密码
     */
    public boolean resetPassword(Integer id, String newPassword) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();
        user.setPassword(newPassword);
        userRepository.save(user);
        return true;
    }

    /**
     * 获取医生用户列表
     */
    public List<User> getDoctors() {
        return getUsersByCategory("医生");
    }

    /**
     * 获取患者用户列表
     */
    public List<User> getPatients() {
        return getUsersByCategory("患者");
    }

    /**
     * 用户是否存在
     */
    public boolean exists(Integer id) {
        return userRepository.existsById(id);
    }
}