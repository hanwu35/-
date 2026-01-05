package com.example.aihealthcheck.entity;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "account", nullable = false, unique = true, length = 50)
    private String account;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "real_name", nullable = false, length = 50)
    private String realName;

    @Column(name = "gender", nullable = false, length = 10)
    private String gender;

    @Column(name = "age", nullable = false)
    private Integer age;

    @Column(name = "category", nullable = false, columnDefinition = "ENUM('患者', '医生')")
    private String category;

    // 构造函数
    public User() {}

    public User(String account, String password, String realName,
                String gender, Integer age, String category) {
        this.account = account;
        this.password = password;
        this.realName = realName;
        this.gender = gender;
        this.age = age;
        this.category = category;
    }

    // Getter 和 Setter
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getAccount() { return account; }
    public void setAccount(String account) { this.account = account; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(userId, user.userId) &&
               Objects.equals(account, user.account);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, account);
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", account='" + account + '\'' +
                ", realName='" + realName + '\'' +
                ", category='" + category + '\'' +
                '}';
    }
}