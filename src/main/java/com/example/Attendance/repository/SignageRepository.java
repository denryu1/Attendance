package com.example.Attendance.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.Attendance.entity.Department;
import com.example.Attendance.entity.Signage;
import com.example.Attendance.entity.User;

@Repository
public interface SignageRepository extends JpaRepository<Signage, Long> {

	// 管理画面用（既存のまま）
	List<Signage> findByTargetUser(User user);

	List<Signage> findByTargetDepartment(Department department);

	List<Signage> findByTargetType(String targetType);

	List<Signage> findByTargetTypeAndTargetDepartment(String targetType, Department department);

	List<Signage> findByTargetTypeAndTargetUser(String targetType, User user);

	// 社員向け表示用（これが本命）
	@Query("""
			    select s from Signage s
			    left join fetch s.targetDepartment
			    left join fetch s.targetUser
			    where s.targetType = 'ALL'
			       or (s.targetType = 'DEPARTMENT' and s.targetDepartment.id = :deptId)
			       or (s.targetType = 'USER' and s.targetUser.id = :userId)
			    order by s.createdAt desc
			""")
	List<Signage> findForUser(@Param("deptId") Long deptId, @Param("userId") Long userId);
}
