package com.example.Attendance.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.Attendance.entity.Attendance;
import com.example.Attendance.entity.User;
import com.example.Attendance.repository.AttendanceRepository;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

	@Autowired
	private AttendanceRepository attendanceRepository;

	@GetMapping("/dashboard")
	public String dashboard(HttpSession session, Model model) {
		String role = (String) session.getAttribute("userRole");
		if (!"ADMIN".equals(role)) {
			return "redirect:/login";
		}

		LocalDate today = LocalDate.now();

		// 今日の全勤怠
		List<Attendance> todayAttendances = attendanceRepository.findByDate(today);

		// ユーザーごとの最新ステータスを集計
		Map<User, String> userStatusMap = todayAttendances.stream()
				.collect(Collectors.groupingBy(
						Attendance::getUser,
						Collectors.collectingAndThen(
								Collectors.maxBy((a1, a2) -> a1.getClockTime().compareTo(a2.getClockTime())),
								opt -> opt.map(Attendance::getClockType).orElse("NOT_CLOCKED"))));

		// 勤務中の人数（CLOCK_OUTしていない人）
		long workingCount = userStatusMap.values().stream()
				.filter(status -> !"CLOCK_OUT".equals(status))
				.count();

		model.addAttribute("todayAttendances", todayAttendances);
		model.addAttribute("userStatusMap", userStatusMap);
		model.addAttribute("workingCount", workingCount);
		model.addAttribute("today", today);

		return "admin/dashboard";
	}

	// 全勤怠一覧
	@GetMapping("/attendance-list")
	public String attendanceList(HttpSession session, Model model) {
		String role = (String) session.getAttribute("userRole");
		if (!"ADMIN".equals(role)) {
			return "redirect:/login";
		}

		List<Attendance> allAttendances = attendanceRepository.findAll();
		model.addAttribute("attendances", allAttendances);

		return "admin/attendance-list";
	}
}