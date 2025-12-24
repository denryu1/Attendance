package com.example.Attendance.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.Attendance.entity.Attendance;
import com.example.Attendance.entity.User;
import com.example.Attendance.repository.AttendanceRepository;
import com.example.Attendance.repository.UserRepository;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

	@Autowired
	private AttendanceRepository attendanceRepository;

	@Autowired
	private UserRepository userRepository;

	@GetMapping("/dashboard")
	public String dashboard(HttpSession session, Model model) {

		String role = (String) session.getAttribute("userRole");
		if (!"ADMIN".equals(role)) {
			return "redirect:/login";
		}

		LocalDate today = LocalDate.now();

		// 今日の勤怠
		List<Attendance> todayAttendances = attendanceRepository.findByDate(today);

		// ユーザーごとの最新ステータス
		Map<User, String> userStatusMap = todayAttendances.stream()
				.collect(Collectors.groupingBy(
						Attendance::getUser,
						Collectors.collectingAndThen(
								Collectors.maxBy(
										(a1, a2) -> a1.getClockTime()
												.compareTo(a2.getClockTime())),
								opt -> opt.map(Attendance::getClockType)
										.orElse("NOT_CLOCKED"))));

		// ユーザーごとの最新勤怠（時刻表示用）
		Map<User, Attendance> userTimeMap = todayAttendances.stream()
				.collect(Collectors.groupingBy(
						Attendance::getUser,
						Collectors.collectingAndThen(
								Collectors.maxBy(
										(a1, a2) -> a1.getClockTime()
												.compareTo(a2.getClockTime())),
								opt -> opt.orElse(null))));

		// カウント系
		long workingCount = userStatusMap.values().stream()
				.filter(s -> "WORKING".equals(s))
				.count();

		long breakCount = userStatusMap.values().stream()
				.filter(s -> "ON_BREAK".equals(s))
				.count();

		long finishedCount = userStatusMap.values().stream()
				.filter(s -> "FINISHED".equals(s))
				.count();

		// 未解決の違反件数を計算
		int unresolvedViolations = calculateUnresolvedViolations();

		// model
		model.addAttribute("today", today);
		model.addAttribute("todayAttendances", todayAttendances);
		model.addAttribute("userStatusMap", userStatusMap);
		model.addAttribute("userTimeMap", userTimeMap);
		model.addAttribute("workingCount", workingCount);
		model.addAttribute("breakCount", breakCount);
		model.addAttribute("finishedCount", finishedCount);
		model.addAttribute("unresolvedViolations", unresolvedViolations);

		return "admin/dashboard";
	}

	@GetMapping("/attendance-list")
	public String attendanceList(
			@RequestParam(required = false) Long userId,
			HttpSession session,
			Model model) {

		String role = (String) session.getAttribute("userRole");
		if (!"ADMIN".equals(role)) {
			return "redirect:/login";
		}

		List<Attendance> attendances;

		// ユーザーIDが指定されている場合はフィルタリング
		if (userId != null) {
			User selectedUser = userRepository.findById(userId).orElse(null);
			if (selectedUser != null) {
				attendances = attendanceRepository.findByUserOrderByClockTimeDesc(selectedUser);
				model.addAttribute("selectedUser", selectedUser);
			} else {
				attendances = attendanceRepository.findAll()
						.stream()
						.sorted((a1, a2) -> a2.getClockTime().compareTo(a1.getClockTime()))
						.collect(Collectors.toList());
			}
		} else {
			// 全て取得
			attendances = attendanceRepository.findAll()
					.stream()
					.sorted((a1, a2) -> a2.getClockTime().compareTo(a1.getClockTime()))
					.collect(Collectors.toList());
		}

		// ユーザー一覧を取得（絞り込み用）
		List<User> allUsers = userRepository.findAll();

		model.addAttribute("attendances", attendances);
		model.addAttribute("allUsers", allUsers);
		model.addAttribute("selectedUserId", userId);

		return "admin/attendance-list";
	}

	// CSVエクスポート機能
	@GetMapping("/attendance-list/export-csv")
	public void exportAttendanceCsv(
			@RequestParam(required = false) Long userId,
			@RequestParam(defaultValue = "all") String period,
			HttpSession session,
			HttpServletResponse response) throws IOException {

		String role = (String) session.getAttribute("userRole");
		if (!"ADMIN".equals(role)) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		LocalDate today = LocalDate.now();
		LocalDate startDate;
		LocalDate endDate = today;

		// 期間の設定
		switch (period) {
		case "today":
			startDate = today;
			break;
		case "week":
			startDate = today.minusWeeks(1);
			break;
		case "month":
			startDate = today.minusMonths(1);
			break;
		default:
			startDate = LocalDate.of(2020, 1, 1); // 全期間
			break;
		}

		// データ取得
		List<Attendance> attendances;
		String fileName;

		if (userId != null) {
			User selectedUser = userRepository.findById(userId).orElse(null);
			if (selectedUser != null) {
				attendances = attendanceRepository.findByUserOrderByClockTimeDesc(selectedUser).stream()
						.filter(a -> !a.getDate().isBefore(startDate) && !a.getDate().isAfter(endDate))
						.collect(Collectors.toList());
				fileName = "attendance_" + selectedUser.getName() + "_" + period + "_"
						+ today.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".csv";
			} else {
				attendances = new ArrayList<>();
				fileName = "attendance_" + period + "_" + today.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
						+ ".csv";
			}
		} else {
			attendances = attendanceRepository.findAll().stream()
					.filter(a -> !a.getDate().isBefore(startDate) && !a.getDate().isAfter(endDate))
					.sorted((a1, a2) -> a2.getClockTime().compareTo(a1.getClockTime()))
					.collect(Collectors.toList());
			fileName = "attendance_all_" + period + "_" + today.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
					+ ".csv";
		}

		// レスポンス設定
		response.setContentType("text/csv; charset=UTF-8");
		response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

		// CSV出力
		PrintWriter writer = response.getWriter();

		// BOM追加（Excel対応）
		writer.write('\uFEFF');

		// ヘッダー
		writer.println("日付,社員名,部署,打刻時刻,打刻種別,場所,備考");

		// データ行
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd (E)");
		DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

		for (Attendance att : attendances) {
			StringBuilder line = new StringBuilder();

			// 日付
			line.append(escapeCSV(att.getDate().format(dateFormatter))).append(",");

			// 社員名
			line.append(escapeCSV(att.getUser().getName())).append(",");

			// 部署
			String department = att.getUser().getDepartment() != null
					? att.getUser().getDepartment().getName()
					: "未所属";
			line.append(escapeCSV(department)).append(",");

			// 打刻時刻
			line.append(escapeCSV(att.getClockTime().format(timeFormatter))).append(",");

			// 打刻種別
			String clockType = "";
			switch (att.getClockType()) {
			case "CLOCK_IN":
				clockType = "出勤";
				break;
			case "CLOCK_OUT":
				clockType = "退勤";
				break;
			case "BREAK_START":
				clockType = "休憩開始";
				break;
			case "BREAK_END":
				clockType = "休憩終了";
				break;
			default:
				clockType = att.getClockType();
			}
			line.append(escapeCSV(clockType)).append(",");

			// 場所
			line.append(escapeCSV(att.getLocation() != null ? att.getLocation() : "")).append(",");

			// 備考
			line.append(escapeCSV(att.getNotes() != null ? att.getNotes() : ""));

			writer.println(line.toString());
		}

		writer.flush();
	}

	// CSVエスケープ処理
	private String escapeCSV(String value) {
		if (value == null) {
			return "";
		}

		// ダブルクォートをエスケープ
		String escaped = value.replace("\"", "\"\"");

		// カンマ、改行、ダブルクォートが含まれている場合はダブルクォートで囲む
		if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\"")) {
			return "\"" + escaped + "\"";
		}

		return escaped;
	}

	@GetMapping("/labor-violations")
	public String laborViolations(
			@RequestParam(defaultValue = "today") String period,
			HttpSession session,
			Model model) {

		String role = (String) session.getAttribute("userRole");
		if (!"ADMIN".equals(role)) {
			return "redirect:/login";
		}

		LocalDate today = LocalDate.now();
		LocalDate startDate;
		LocalDate endDate = today;

		// 期間の設定
		switch (period) {
		case "week":
			startDate = today.minusWeeks(1);
			break;
		case "month":
			startDate = today.minusMonths(1);
			break;
		default:
			startDate = today;
			break;
		}

		// 対象期間の勤怠データを取得
		List<Attendance> attendances = attendanceRepository.findAll().stream()
				.filter(a -> !a.getDate().isBefore(startDate) && !a.getDate().isAfter(endDate))
				.collect(Collectors.toList());

		// ユーザーごとにグループ化
		Map<User, List<Attendance>> userAttendances = attendances.stream()
				.collect(Collectors.groupingBy(Attendance::getUser));

		// 違反レポートを作成
		List<ViolationReport> violationReports = new ArrayList<>();

		for (Map.Entry<User, List<Attendance>> entry : userAttendances.entrySet()) {
			User user = entry.getKey();
			List<Attendance> userRecords = entry.getValue();

			ViolationReport report = checkViolations(user, userRecords, startDate, endDate);
			if (report.hasViolations()) {
				violationReports.add(report);
			}
		}

		model.addAttribute("violationReports", violationReports);
		model.addAttribute("totalViolations", violationReports.size());
		model.addAttribute("period", period);
		model.addAttribute("startDate", startDate);
		model.addAttribute("endDate", endDate);

		return "admin/labor-violations";
	}

	// 違反を解決済みにする
	@PostMapping("/labor-violations/resolve/{userId}/{date}")
	public String resolveViolation(
			@PathVariable Long userId,
			@PathVariable String date,
			@RequestParam(defaultValue = "today") String period,
			HttpSession session) {

		String role = (String) session.getAttribute("userRole");
		if (!"ADMIN".equals(role)) {
			return "redirect:/login";
		}

		User user = userRepository.findById(userId).orElse(null);
		LocalDate violationDate = LocalDate.parse(date);

		if (user != null) {
			// 該当日のすべての勤怠記録を解決済みにする
			List<Attendance> attendances = attendanceRepository.findByUserAndDate(user, violationDate);
			for (Attendance attendance : attendances) {
				attendance.setViolationResolved(true);
				attendanceRepository.save(attendance);
			}
		}

		return "redirect:/admin/labor-violations?period=" + period;
	}

	// 未解決の違反件数を計算
	private int calculateUnresolvedViolations() {
		LocalDate today = LocalDate.now();
		LocalDate startDate = today.minusMonths(1); // 過去1ヶ月

		List<Attendance> attendances = attendanceRepository.findAll().stream()
				.filter(a -> !a.getDate().isBefore(startDate) && !a.getDate().isAfter(today))
				.collect(Collectors.toList());

		Map<User, List<Attendance>> userAttendances = attendances.stream()
				.collect(Collectors.groupingBy(Attendance::getUser));

		int unresolvedCount = 0;

		for (Map.Entry<User, List<Attendance>> entry : userAttendances.entrySet()) {
			User user = entry.getKey();
			List<Attendance> userRecords = entry.getValue();

			ViolationReport report = checkViolations(user, userRecords, startDate, today);
			if (report.hasUnresolvedViolations()) {
				unresolvedCount++;
			}
		}

		return unresolvedCount;
	}

	// 違反チェックのヘルパーメソッド
	private ViolationReport checkViolations(User user, List<Attendance> records, LocalDate startDate,
			LocalDate endDate) {
		ViolationReport report = new ViolationReport();
		report.setUser(user);

		// 日付ごとにグループ化
		Map<LocalDate, List<Attendance>> dailyRecords = records.stream()
				.collect(Collectors.groupingBy(Attendance::getDate));

		List<DailyViolation> dailyViolations = new ArrayList<>();
		double weeklyTotalMinutes = 0;

		for (Map.Entry<LocalDate, List<Attendance>> entry : dailyRecords.entrySet()) {
			LocalDate date = entry.getKey();
			List<Attendance> dayRecords = entry.getValue()
					.stream()
					.sorted(Comparator.comparing(Attendance::getClockTime))
					.collect(Collectors.toList());

			DailyViolation daily = calculateDailyViolation(date, dayRecords);
			if (daily.hasViolation()) {
				dailyViolations.add(daily);
			}

			weeklyTotalMinutes += daily.getWorkMinutes();
		}

		report.setDailyViolations(dailyViolations);

		// 週の労働時間をチェック（40時間 = 2400分）
		double weeklyHours = weeklyTotalMinutes / 60.0;
		report.setWeeklyHours(weeklyHours);
		report.setWeeklyOvertime(weeklyHours > 40);

		return report;
	}

	private DailyViolation calculateDailyViolation(LocalDate date, List<Attendance> records) {
		DailyViolation violation = new DailyViolation();
		violation.setDate(date);

		LocalDateTime clockIn = null;
		LocalDateTime clockOut = null;
		int breakMinutes = 0;
		LocalDateTime breakStart = null;
		boolean isResolved = false;

		for (Attendance record : records) {
			// 一つでも解決済みフラグがあればこの日は解決済み
			if (record.getViolationResolved() != null && record.getViolationResolved()) {
				isResolved = true;
			}

			switch (record.getClockType()) {
			case "CLOCK_IN":
				clockIn = record.getClockTime();
				break;
			case "CLOCK_OUT":
				clockOut = record.getClockTime();
				break;
			case "BREAK_START":
				breakStart = record.getClockTime();
				break;
			case "BREAK_END":
				if (breakStart != null) {
					breakMinutes += java.time.Duration.between(breakStart, record.getClockTime()).toMinutes();
					breakStart = null;
				}
				break;
			}
		}

		violation.setClockIn(clockIn);
		violation.setClockOut(clockOut);
		violation.setBreakMinutes(breakMinutes);
		violation.setResolved(isResolved);

		// 出勤したのに退勤していないチェック
		LocalDate today = LocalDate.now();
		LocalDateTime now = LocalDateTime.now();

		if (clockIn != null && clockOut == null) {
			if (date.equals(today)) {
				// 今日の場合：出勤から12時間以上経過している場合は違反
				long hoursSinceClockIn = java.time.Duration.between(clockIn, now).toHours();
				if (hoursSinceClockIn >= 12) {
					violation.setNoClockOut(true);
					violation.setHoursSinceClockIn((int) hoursSinceClockIn);
				}
			} else {
				// 過去の日付で退勤記録がない場合も違反
				violation.setNoClockOut(true);
				long hoursSinceClockIn = java.time.Duration.between(clockIn, date.plusDays(1).atStartOfDay()).toHours();
				violation.setHoursSinceClockIn((int) hoursSinceClockIn);
			}
		}

		// 労働時間を計算（退勤済みの場合のみ）
		if (clockIn != null && clockOut != null) {
			long totalMinutes = java.time.Duration.between(clockIn, clockOut).toMinutes();
			long workMinutes = totalMinutes - breakMinutes;
			violation.setWorkMinutes((int) workMinutes);

			// 8時間（480分）超過チェック
			if (workMinutes > 480) {
				violation.setOvertimeViolation(true);
				violation.setOvertimeMinutes((int) (workMinutes - 480));
			}

			// 休憩時間の法定要件チェック
			int requiredBreakMinutes = 0;
			if (workMinutes > 360) { // 6時間超
				requiredBreakMinutes = 45;
			}
			if (workMinutes > 480) { // 8時間超
				requiredBreakMinutes = 60;
			}

			violation.setRequiredBreakMinutes(requiredBreakMinutes);
			if (breakMinutes < requiredBreakMinutes) {
				violation.setBreakViolation(true);
			}
		}

		return violation;
	}

	// 内部クラス
	public static class ViolationReport {
		private User user;
		private List<DailyViolation> dailyViolations = new ArrayList<>();
		private double weeklyHours;
		private boolean weeklyOvertime;

		public boolean hasViolations() {
			return !dailyViolations.isEmpty() || weeklyOvertime;
		}

		public boolean hasUnresolvedViolations() {
			if (weeklyOvertime)
				return true;
			return dailyViolations.stream().anyMatch(dv -> !dv.isResolved());
		}

		// Getters and Setters
		public User getUser() {
			return user;
		}

		public void setUser(User user) {
			this.user = user;
		}

		public List<DailyViolation> getDailyViolations() {
			return dailyViolations;
		}

		public void setDailyViolations(List<DailyViolation> dailyViolations) {
			this.dailyViolations = dailyViolations;
		}

		public double getWeeklyHours() {
			return weeklyHours;
		}

		public void setWeeklyHours(double weeklyHours) {
			this.weeklyHours = weeklyHours;
		}

		public boolean isWeeklyOvertime() {
			return weeklyOvertime;
		}

		public void setWeeklyOvertime(boolean weeklyOvertime) {
			this.weeklyOvertime = weeklyOvertime;
		}
	}

	public static class DailyViolation {
		private LocalDate date;
		private LocalDateTime clockIn;
		private LocalDateTime clockOut;
		private int workMinutes;
		private int breakMinutes;
		private boolean overtimeViolation;
		private int overtimeMinutes;
		private boolean breakViolation;
		private int requiredBreakMinutes;
		private boolean noClockOut;
		private int hoursSinceClockIn;
		private boolean resolved;

		public boolean hasViolation() {
			return overtimeViolation || breakViolation || noClockOut;
		}

		// Getters and Setters
		public LocalDate getDate() {
			return date;
		}

		public void setDate(LocalDate date) {
			this.date = date;
		}

		public LocalDateTime getClockIn() {
			return clockIn;
		}

		public void setClockIn(LocalDateTime clockIn) {
			this.clockIn = clockIn;
		}

		public LocalDateTime getClockOut() {
			return clockOut;
		}

		public void setClockOut(LocalDateTime clockOut) {
			this.clockOut = clockOut;
		}

		public int getWorkMinutes() {
			return workMinutes;
		}

		public void setWorkMinutes(int workMinutes) {
			this.workMinutes = workMinutes;
		}

		public int getBreakMinutes() {
			return breakMinutes;
		}

		public void setBreakMinutes(int breakMinutes) {
			this.breakMinutes = breakMinutes;
		}

		public boolean isOvertimeViolation() {
			return overtimeViolation;
		}

		public void setOvertimeViolation(boolean overtimeViolation) {
			this.overtimeViolation = overtimeViolation;
		}

		public int getOvertimeMinutes() {
			return overtimeMinutes;
		}

		public void setOvertimeMinutes(int overtimeMinutes) {
			this.overtimeMinutes = overtimeMinutes;
		}

		public boolean isBreakViolation() {
			return breakViolation;
		}

		public void setBreakViolation(boolean breakViolation) {
			this.breakViolation = breakViolation;
		}

		public int getRequiredBreakMinutes() {
			return requiredBreakMinutes;
		}

		public void setRequiredBreakMinutes(int requiredBreakMinutes) {
			this.requiredBreakMinutes = requiredBreakMinutes;
		}

		public boolean isNoClockOut() {
			return noClockOut;
		}

		public void setNoClockOut(boolean noClockOut) {
			this.noClockOut = noClockOut;
		}

		public int getHoursSinceClockIn() {
			return hoursSinceClockIn;
		}

		public void setHoursSinceClockIn(int hoursSinceClockIn) {
			this.hoursSinceClockIn = hoursSinceClockIn;
		}

		public boolean isResolved() {
			return resolved;
		}

		public void setResolved(boolean resolved) {
			this.resolved = resolved;
		}
	}
}