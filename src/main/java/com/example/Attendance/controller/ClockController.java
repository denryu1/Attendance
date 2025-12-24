package com.example.Attendance.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.Attendance.entity.Attendance;
import com.example.Attendance.entity.User;
import com.example.Attendance.repository.AttendanceRepository;
import com.example.Attendance.repository.UserRepository;

@Controller
@RequestMapping("/attendance")
public class ClockController {

	@Autowired
	private AttendanceRepository attendanceRepository;

	@Autowired
	private UserRepository userRepository;

	// 打刻画面
	@GetMapping("/clock")
	public String clockPage(HttpSession session, Model model) {
		Long userId = (Long) session.getAttribute("userId");
		if (userId == null) {
			return "redirect:/login";
		}

		User user = userRepository.findById(userId).orElse(null);
		if (user == null) {
			return "redirect:/login";
		}

		// 今日の打刻履歴を取得
		LocalDate today = LocalDate.now();
		List<Attendance> todayAttendances = attendanceRepository.findByUserAndDateOrderByClockTimeAsc(user, today);

		// 最新の打刻タイプを取得
		String currentStatus = "NOT_CLOCKED"; // 未出勤
		if (!todayAttendances.isEmpty()) {
			String lastClockType = todayAttendances.get(todayAttendances.size() - 1).getClockType();
			switch (lastClockType) {
			case "CLOCK_IN":
				currentStatus = "WORKING"; // 勤務中
				break;
			case "BREAK_START":
				currentStatus = "ON_BREAK"; // 休憩中
				break;
			case "BREAK_END":
				currentStatus = "WORKING"; // 勤務中（休憩から戻った）
				break;
			case "CLOCK_OUT":
				currentStatus = "FINISHED"; // 退勤済み
				break;
			}
		}

		model.addAttribute("user", user);
		model.addAttribute("todayAttendances", todayAttendances);
		model.addAttribute("currentStatus", currentStatus);

		return "attendance/clock";
	}

	// 出勤打刻
	@PostMapping("/clock-in")
	@ResponseBody
	public String clockIn(HttpSession session) {
		Long userId = (Long) session.getAttribute("userId");
		if (userId == null) {
			return "{\"success\":false,\"message\":\"ログインしてください\"}";
		}

		User user = userRepository.findById(userId).orElse(null);
		if (user == null) {
			return "{\"success\":false,\"message\":\"ユーザーが見つかりません\"}";
		}

		// 出勤記録作成
		Attendance attendance = Attendance.builder()
				.user(user)
				.date(LocalDate.now())
				.clockTime(LocalDateTime.now())
				.clockType("CLOCK_IN")
				.location("オフィス")
				.build();

		attendanceRepository.save(attendance);

		return "{\"success\":true,\"message\":\"出勤しました\"}";
	}

	// 休憩開始
	@PostMapping("/break-start")
	@ResponseBody
	public String breakStart(HttpSession session) {
		Long userId = (Long) session.getAttribute("userId");
		if (userId == null) {
			return "{\"success\":false,\"message\":\"ログインしてください\"}";
		}

		User user = userRepository.findById(userId).orElse(null);
		if (user == null) {
			return "{\"success\":false,\"message\":\"ユーザーが見つかりません\"}";
		}

		Attendance attendance = Attendance.builder()
				.user(user)
				.date(LocalDate.now())
				.clockTime(LocalDateTime.now())
				.clockType("BREAK_START")
				.location("オフィス")
				.build();

		attendanceRepository.save(attendance);

		return "{\"success\":true,\"message\":\"休憩を開始しました\"}";
	}

	// 休憩終了
	@PostMapping("/break-end")
	@ResponseBody
	public String breakEnd(HttpSession session) {
		Long userId = (Long) session.getAttribute("userId");
		if (userId == null) {
			return "{\"success\":false,\"message\":\"ログインしてください\"}";
		}

		User user = userRepository.findById(userId).orElse(null);
		if (user == null) {
			return "{\"success\":false,\"message\":\"ユーザーが見つかりません\"}";
		}

		Attendance attendance = Attendance.builder()
				.user(user)
				.date(LocalDate.now())
				.clockTime(LocalDateTime.now())
				.clockType("BREAK_END")
				.location("オフィス")
				.build();

		attendanceRepository.save(attendance);

		return "{\"success\":true,\"message\":\"休憩から戻りました\"}";
	}

	// 退勤打刻
	@PostMapping("/clock-out")
	@ResponseBody
	public String clockOut(HttpSession session) {
		Long userId = (Long) session.getAttribute("userId");
		if (userId == null) {
			return "{\"success\":false,\"message\":\"ログインしてください\"}";
		}

		User user = userRepository.findById(userId).orElse(null);
		if (user == null) {
			return "{\"success\":false,\"message\":\"ユーザーが見つかりません\"}";
		}

		Attendance attendance = Attendance.builder()
				.user(user)
				.date(LocalDate.now())
				.clockTime(LocalDateTime.now())
				.clockType("CLOCK_OUT")
				.location("オフィス")
				.build();

		attendanceRepository.save(attendance);

		return "{\"success\":true,\"message\":\"お疲れ様でした\"}";
	}

	// 個人の勤怠履歴
	@GetMapping("/history")
	public String history(HttpSession session, Model model) {
		Long userId = (Long) session.getAttribute("userId");
		if (userId == null) {
			return "redirect:/login";
		}

		User user = userRepository.findById(userId)
				.orElseThrow(() -> new RuntimeException("User not found"));

		List<Attendance> attendances = attendanceRepository.findByUserOrderByClockTimeDesc(user);

		// ===== 追加：集計処理 =====
		int totalMinutes = attendances.stream()
				.mapToInt(att -> {
					// CLOCK_IN / CLOCK_OUT ベースで計算できないため一旦 0
					// ※今後 start/end 構造にするならここを拡張
					return 0;
				})
				.sum();

		double totalHours = totalMinutes / 60.0;

		long workDays = attendances.stream()
				.map(Attendance::getDate)
				.distinct()
				.count();
		// ========================

		model.addAttribute("user", user);
		model.addAttribute("attendances", attendances);
		model.addAttribute("totalHours", totalHours);
		model.addAttribute("workDays", workDays);

		return "attendance/history";
	}

}
