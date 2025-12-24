package com.example.Attendance.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "signages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Signage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id; // メッセージID

	@Column(nullable = false, length = 255)
	private String title; // タイトル

	@Column(nullable = false, columnDefinition = "TEXT")
	private String message; // 本文

	@Column(length = 20)
	private String targetType; // 送信対象者

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "target_department_id")
	private Department targetDepartment;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "target_user_id")
	private User targetUser;

	private LocalDateTime createdAt; // 作成日
	private LocalDateTime updatedAt; // 更新日
}
