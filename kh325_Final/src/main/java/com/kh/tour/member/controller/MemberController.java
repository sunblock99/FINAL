package com.kh.tour.member.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;

import com.kh.tour.member.model.service.MemberService;
import com.kh.tour.member.model.vo.Member;

import lombok.extern.slf4j.Slf4j;


@Slf4j // log용도
@SessionAttributes("loginMember") // loingMember를 model에서 취급할때 Session으로 처리하는 파라메터
@Controller
public class MemberController {

	@Autowired
	private MemberService service;
	
	@PostMapping("/login")
	public String login(Model model, String userId, String userPwd) {
		log.info("id : " + userId + ", pw : " + userPwd);
		Member loginMember = service.login(userId, userPwd);
		
		if(loginMember != null) {
			model.addAttribute("loginMember", loginMember);
			return "redirect:/";
		}else {
			model.addAttribute("msg", "아이디나 패스워드가 잘못되었습니다.");
			model.addAttribute("location", "/");
			return "common/msg";
		}
	}
	
	@RequestMapping("/logout")
	public String logout(SessionStatus status) {
		log.info("status : " + status.isComplete());
		status.setComplete(); // 세션을 종료하는 코드
		log.info("status : " + status.isComplete());
		return "redirect:/";
	}
	
	@GetMapping("/member/enroll")
	public String enrollPage() {
		log.info("가입 페이지 요청");
		return "member/enroll";
	}
	
	// ModelAndView 사용법, 가능하면 하나 통일해서 쓸것!! ModelAndView=전자정부에서 좋아한다.....
	@PostMapping("/member/enroll")
	public ModelAndView enroll(ModelAndView model, @ModelAttribute Member member) {
		log.info("회원 가입, member : " + member);
		int result = 0;
		try {
			result = service.save(member);
		} catch (Exception e) {
			result = -1;
		}
		if(result > 0) {
			model.addObject("msg", "회원가입에 성공하였습니다.");
			model.addObject("location", "/");
		}else {
			model.addObject("msg", "회원가입에 실패하였습니다. 다시 한번 확인해주세요.");
			model.addObject("location", "/");
		}
		model.setViewName("common/msg");
		return model;
	}
	
	// AJAX 대응용 코드
	// ResponseEntity : json 객체를 알아서 만들어 주는 리턴값
	@GetMapping("/member/idCheck")
	public ResponseEntity<Map<String,Object>> idCheck(String id){
		log.info("아이디 중복 확인, user id : "+ id);
		
		boolean result = service.validate(id);
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("validate", result);
		
		return new ResponseEntity<Map<String,Object>>(map, HttpStatus.OK);
	}
	
	
	@GetMapping("/member/view")
	public String view() {
		log.info("회원 정보 페이지 요청");
		return "member/view";
	}
	
//	@SessionAttribute : 세션으로 가지고 있는 값을 command 객체로 땡겨올때 쓰는 어노테이션
	@PostMapping("/member/update")
	public String update(Model model,
				@ModelAttribute Member member, // @ModelAttribute 생략 가능!!
				@SessionAttribute(name= "loginMember", required = false) Member loginMember 
			) {
		if(loginMember == null || loginMember.getUserEmail().equals(member.getUserEmail()) == false) {
			model.addAttribute("msg", "잘못된 접근입니다.");
			model.addAttribute("location", "/");
			return "common/msg";
		}
		
		member.setUserNo(loginMember.getUserNo());
		int result = service.save(member);
		
		if(result > 0 ) {
			model.addAttribute("loginMember", service.findById(member.getUserEmail())); // DB에 있는 값으로 다시 세션값 업데이트
			model.addAttribute("msg", "회원정보를 수정하였습니다.");
			model.addAttribute("location", "/member/view");
		}else {
			model.addAttribute("msg", "회원정보 수정을 실패하였습니다.");
			model.addAttribute("location", "/member/view");
		}
		
		return "common/msg";
	}
	
	
	@GetMapping("/member/delete")
	public String delete(Model model,
			@SessionAttribute(name= "loginMember", required = false) Member loginMember) {
		int result = service.delete(loginMember.getUserNo());
		
		if(result > 0 ) {
			model.addAttribute("msg", "정상적으로 탈퇴 되었습니다.");
			model.addAttribute("location", "/logout");
		}else {
			model.addAttribute("msg", "회원 탈퇴에 실패하였습니다.");
			model.addAttribute("location", "/member/view");
		}
		return "common/msg";
	}
	
	@GetMapping("/member/updatePwd")
	public String updatePwd() {
		return "/member/updatePwd";
	}
	
	
	@PostMapping("/member/updatePwd")
	public String updatePwd(Model model,
			@SessionAttribute(name= "loginMember", required = false) Member loginMember,
			String userPwd) {
		log.info("update pw : " + userPwd);
		
		int result = service.updatePwd(loginMember, userPwd);
		
		if(result > 0 ) {
			model.addAttribute("msg", "정상적으로 수정되었습니다.");
		}else {
			model.addAttribute("msg", "비밀번호 변경에 실패하였습니다.");
		}
		model.addAttribute("script", "self.close()");
		return "common/msg";
	}
	
	@GetMapping("/kakao/callback")
	public String kakaoLogin(Model model,String code) throws Exception{
		// code는 카카오 서버로부터 받은 인가코드
		
		String access_Token = service.getAccessToken(code);
		System.out.println("###access_Token#### : " + access_Token);
		
		
		
		Member userInfo = service.getUserInfo(access_Token);
		System.out.println("###uNo#### : " + userInfo.getUserNo());
		System.out.println("###nickname#### : " + userInfo.getUserName());
		System.out.println("###email#### : " + userInfo.getUserEmail());
		
		
		
		model.addAttribute("loginMember", userInfo);
		
		return "home";
	}
	
}











