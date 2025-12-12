package com.example.Attendance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "departments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id; // 部署id

    @Column(nullable = false, length = 100)
    private String name; // 部署名

    // ユーザーとのリレーション（双方向）
    @OneToMany(mappedBy = "department")
    private List<User> users;
}
