package com.example.Attendance.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.Attendance.entity.Department;
import com.example.Attendance.entity.Signage;
import com.example.Attendance.entity.User;

@Repository
public interface SignageRepository extends JpaRepository<Signage, Long> {

	// 個人宛メッセージを取得
	List<Signage> findByTargetUser(User user);

	// 部署宛メッセージを取得
	List<Signage> findByTargetDepartment(Department department);

	// targetType（"user" / "department" / "all"など）で一覧取得
	List<Signage> findByTargetType(String targetType);

	// target_type と 部署ID を組み合わせたメッセージ取得
	List<Signage> findByTargetTypeAndTargetDepartment(String targetType, Department department);

	// target_type と ユーザーID を組み合わせたメッセージ取得
	List<Signage> findByTargetTypeAndTargetUser(String targetType, User user);
}
