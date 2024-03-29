package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "business_order")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String description;
    private String sum;

    private String deadline;

    @OneToOne
    @JoinColumn(name = "business_user_info_id", referencedColumnName = "id")
    private BusinessUserInfo businessUserInfo;


}
