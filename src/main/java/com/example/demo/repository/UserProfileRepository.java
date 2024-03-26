package com.example.demo.repository;

import com.example.demo.model.UserInfo;
import com.example.demo.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile,Long> {
    UserProfile findByUserInfo(UserInfo userInfo);

}
