package com.example.Attendance.controller;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.Attendance.entity.Department;
import com.example.Attendance.entity.Signage;
import com.example.Attendance.entity.User;
import com.example.Attendance.repository.DepartmentRepository;
import com.example.Attendance.repository.SignageRepository;
import com.example.Attendance.repository.UserRepository;

@Controller
@RequestMapping("/signages")
public class SignageViewController {

	@Autowired
	private SignageRepository signageRepository;

	@Autowired
	private DepartmentRepository departmentRepository;

	@Autowired
	private UserRepository userRepository;

	// サイネージ表示(社員向け)
	@GetMapping("/display")
	public String display(Model model) {
		List<Signage> activeSignages = signageRepository.findAll().stream()
				.filter(s -> "ALL".equals(s.getTargetType()))
				.toList();

		model.addAttribute("signages", activeSignages);
		return "signages/display";
	}

	// サイネージ一覧(管理者向け) - 絞り込み機能追加
	@GetMapping("/list")
	public String list(
			@RequestParam(required = false) String targetType,
			@RequestParam(required = false) Long departmentId,
			@RequestParam(required = false) Long userId,
			HttpSession session,
			Model model) {

		String role = (String) session.getAttribute("userRole");
		if (!"ADMIN".equals(role)) {
			return "redirect:/login";
		}

		List<Signage> signages;
		Department selectedDepartment = null;
		User selectedUser = null;

		// 絞り込み処理
		if (targetType != null && !targetType.isEmpty()) {
			switch (targetType) {
			case "ALL":
				signages = signageRepository.findByTargetType("ALL");
				break;
			case "DEPARTMENT":
				if (departmentId != null) {
					selectedDepartment = departmentRepository.findById(departmentId).orElse(null);
					if (selectedDepartment != null) {
						signages = signageRepository.findByTargetTypeAndTargetDepartment("DEPARTMENT",
								selectedDepartment);
					} else {
						signages = signageRepository.findByTargetType("DEPARTMENT");
					}
				} else {
					signages = signageRepository.findByTargetType("DEPARTMENT");
				}
				break;
			case "USER":
				if (userId != null) {
					selectedUser = userRepository.findById(userId).orElse(null);
					if (selectedUser != null) {
						signages = signageRepository.findByTargetTypeAndTargetUser("USER", selectedUser);
					} else {
						signages = signageRepository.findByTargetType("USER");
					}
				} else {
					signages = signageRepository.findByTargetType("USER");
				}
				break;
			default:
				signages = signageRepository.findAll();
				break;
			}
		} else {
			// 絞り込みなし - 全て表示
			signages = signageRepository.findAll();
		}

		// 統計データを計算
		List<Signage> allSignages = signageRepository.findAll();
		long allCount = allSignages.stream()
				.filter(s -> "ALL".equals(s.getTargetType()))
				.count();
		long departmentCount = allSignages.stream()
				.filter(s -> "DEPARTMENT".equals(s.getTargetType()))
				.count();
		long userCount = allSignages.stream()
				.filter(s -> "USER".equals(s.getTargetType()))
				.count();

		// 絞り込み用のリスト
		List<Department> allDepartments = departmentRepository.findAll();
		List<User> allUsers = userRepository.findAll();

		model.addAttribute("signages", signages);
		model.addAttribute("allCount", allCount);
		model.addAttribute("departmentCount", departmentCount);
		model.addAttribute("userCount", userCount);

		// 絞り込み関連
		model.addAttribute("allDepartments", allDepartments);
		model.addAttribute("allUsers", allUsers);
		model.addAttribute("selectedTargetType", targetType);
		model.addAttribute("selectedDepartmentId", departmentId);
		model.addAttribute("selectedUserId", userId);
		model.addAttribute("selectedDepartment", selectedDepartment);
		model.addAttribute("selectedUser", selectedUser);

		return "signages/list";
	}

	// サイネージ作成画面
	@GetMapping("/create")
	public String createForm(HttpSession session, Model model) {
		String role = (String) session.getAttribute("userRole");
		if (!"ADMIN".equals(role)) {
			return "redirect:/login";
		}

		model.addAttribute("signage", new Signage());

		// 部署一覧とユーザー一覧を渡す
		List<Department> departments = departmentRepository.findAll();
		List<User> users = userRepository.findAll();

		model.addAttribute("departments", departments);
		model.addAttribute("users", users);

		return "signages/create";
	}

	// サイネージ作成処理（統一版 - Long型のみ使用）
	@PostMapping("/create")
	public String create(
			HttpSession session,
			@RequestParam String title,
			@RequestParam String message,
			@RequestParam(defaultValue = "ALL") String targetType,
			@RequestParam(required = false) Long targetDepartmentId,
			@RequestParam(required = false) Long targetUserId) {

		String role = (String) session.getAttribute("userRole");
		if (!"ADMIN".equals(role)) {
			return "redirect:/login";
		}

		Department targetDepartment = null;
		User targetUser = null;

		// 部署またはユーザーを取得
		if ("DEPARTMENT".equals(targetType) && targetDepartmentId != null) {
			targetDepartment = departmentRepository.findById(targetDepartmentId).orElse(null);
			System.out.println(
					"Selected Department: " + (targetDepartment != null ? targetDepartment.getName() : "null"));
		}
		if ("USER".equals(targetType) && targetUserId != null) {
			targetUser = userRepository.findById(targetUserId).orElse(null);
			System.out.println("Selected User: " + (targetUser != null ? targetUser.getName() : "null"));
		}

		Signage signage = Signage.builder()
				.title(title)
				.message(message)
				.targetType(targetType)
				.targetDepartment(targetDepartment)
				.targetUser(targetUser)
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();

		signageRepository.save(signage);

		System.out.println(
				"Signage created - Type: " + targetType + ", Dept: " + targetDepartment + ", User: " + targetUser);

		return "redirect:/signages/list";
	}

	// サイネージ削除
	@PostMapping("/delete/{id}")
	public String delete(@PathVariable Long id, HttpSession session) {
		String role = (String) session.getAttribute("userRole");
		if (!"ADMIN".equals(role)) {
			return "redirect:/login";
		}

		signageRepository.deleteById(id);
		return "redirect:/signages/list";
	}
}