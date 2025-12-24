package com.example.Attendance.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "attendance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attendance {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false)
	private LocalDate date; // 勤務日

	@Column(name = "clock_time", nullable = false)
	private LocalDateTime clockTime; // 打刻時刻

	@Column(name = "clock_type", nullable = false, length = 20)
	private String clockType; // CLOCK_IN(出勤), CLOCK_OUT(退勤), BREAK_START(休憩開始), BREAK_END(休憩終了)

	@Column(length = 100)
	private String location; // 打刻場所

	@Column(columnDefinition = "TEXT")
	private String notes; // 備考

	@Column(name = "violation_resolved", nullable = false)
	@Builder.Default
	private Boolean violationResolved = false; // 違反解決済みフラグ

	@PrePersist
	protected void onCreate() {
		if (clockTime == null) {
			clockTime = LocalDateTime.now();
		}
		if (date == null) {
			date = clockTime.toLocalDate();
		}
		if (violationResolved == null) {
			violationResolved = false;
		}
	}
}