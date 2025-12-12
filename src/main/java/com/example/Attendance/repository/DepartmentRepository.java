package com.example.Attendance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.Attendance.entity.Department;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

	// 部署名で部署を取得（部署検索やバリデーション用）
	Department findByName(String name);
}
