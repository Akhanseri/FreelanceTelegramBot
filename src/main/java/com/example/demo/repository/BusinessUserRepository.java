package com.example.demo.repository;

import com.example.demo.model.BusinessUserInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BusinessUserRepository extends JpaRepository<BusinessUserInfo,Long> {
    BusinessUserInfo getByChatId(Long id);
}
