package com.example.Attendance.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.Attendance.entity.Attendance;
import com.example.Attendance.entity.User;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

	// 特定ユーザーの勤怠一覧を取得
	List<Attendance> findByUser(User user);

	// 特定ユーザー・特定日の勤怠（出勤済みチェックなどに使う）
	List<Attendance> findByUserAndDate(User user, LocalDate date);

	// 特定ユーザー・特定日の勤怠を時刻順で取得
	List<Attendance> findByUserAndDateOrderByClockTimeAsc(User user, LocalDate date);

	// 特定日の勤怠を取得（管理者の「今日の勤怠一覧」などで使う）
	List<Attendance> findByDate(LocalDate date);

	// 特定ユーザーの最新の打刻を取得
	@Query("SELECT a FROM Attendance a WHERE a.user = :user ORDER BY a.clockTime DESC")
	List<Attendance> findByUserOrderByClockTimeDesc(@Param("user") User user);

	// 今日の特定ユーザーの最新打刻タイプを取得
	@Query("SELECT a FROM Attendance a WHERE a.user = :user AND a.date = :date ORDER BY a.clockTime DESC")
	List<Attendance> findTodayLatestByUser(@Param("user") User user, @Param("date") LocalDate date);
}