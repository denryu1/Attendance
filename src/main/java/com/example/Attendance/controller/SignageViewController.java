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

import com.example.Attendance.entity.Signage;
import com.example.Attendance.repository.SignageRepository;

@Controller
@RequestMapping("/signages")
public class SignageViewController {

	@Autowired
	private SignageRepository signageRepository;

	// サイネージ表示（社員向け）
	@GetMapping("/display")
	public String display(Model model) {
		List<Signage> activeSignages = signageRepository.findAll().stream()
				.filter(s -> "ALL".equals(s.getTargetType()))
				.toList();

		model.addAttribute("signages", activeSignages);
		return "signages/display";
	}

	// サイネージ一覧（管理者向け）
	@GetMapping("/list")
	public String list(HttpSession session, Model model) {
		String role = (String) session.getAttribute("userRole");
		if (!"ADMIN".equals(role)) {
			return "redirect:/login";
		}

		List<Signage> signages = signageRepository.findAll();
		model.addAttribute("signages", signages);
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
		return "signages/create";
	}

	// サイネージ作成処理
	@PostMapping("/create")
	public String create(
			HttpSession session,
			@RequestParam String title,
			@RequestParam String message,
			@RequestParam(defaultValue = "ALL") String targetType) {

		String role = (String) session.getAttribute("userRole");
		if (!"ADMIN".equals(role)) {
			return "redirect:/login";
		}

		Signage signage = Signage.builder()
				.title(title)
				.message(message)
				.targetType(targetType)
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();

		signageRepository.save(signage);

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