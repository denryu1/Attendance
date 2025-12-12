package com.example.Attendance.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.Attendance.entity.Department;
import com.example.Attendance.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

	// メールアドレスからユーザーを取得（ログイン等で利用）
	Optional<User> findByEmail(String email);

	// 指定した部署に所属するユーザー一覧を取得
	List<User> findByDepartment(Department department);

	// 権限（管理者・一般など）でユーザーを取得
	List<User> findByRole(String role);
}
