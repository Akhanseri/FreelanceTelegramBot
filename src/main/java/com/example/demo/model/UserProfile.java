package com.example.demo.model;

import com.example.demo.model.UserInfo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "user_profile")
public class UserProfile {
    @Id
    @GeneratedValue(strategy =GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String number;
    private String specialization;

    private String description;

    private String projects;

    private String money;

    @OneToOne
    @JoinColumn(name = "user_info_id", referencedColumnName = "id")
    private UserInfo userInfo;

}
