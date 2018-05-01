package edu.sjsu.cmpe275.project.controllers;

import java.util.Random;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.annotation.JsonView;

import edu.sjsu.cmpe275.project.View;
import edu.sjsu.cmpe275.project.entities.Account;
import edu.sjsu.cmpe275.project.repositories.AccountRepository;
import edu.sjsu.cmpe275.project.services.NotificationService;

@Controller
public class AccountController {
	
	@Autowired
	private AccountRepository accountRepo;
	
	@Autowired
	private NotificationService notifService;
	/*
	@GetMapping("/")
	public String showPage(Model model, @RequestParam(defaultValue="1")int page) {
		model.addAttribute("data", accountRepo.findAll(new PageRequest(page, 4)));
		return "index";
	}
	*/
	
	@GetMapping("/")
	public String showIndex() {
		
		return "index";
	}
	
	@GetMapping("/signup")
	public String showPage(Model model) {
		model.addAttribute("account", new Account());
		
		return "signup";
	}
	
	
	@PostMapping("/account")
	public String saveAccount(@Valid @ModelAttribute("account") Account account, BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
            return "signup";
        }
		
		accountRepo.save(account);
		//send notification
		try {
			notifService.sendNotification(account.getEmail());
		}
		catch(MailException e) {
			//catch error
		}
		
		return "redirect:/";
	}
	
	@GetMapping("/account/{id}")
	@JsonView(View.Account.class)
	public ResponseEntity<Account> getAccount(@PathVariable int id){
		Account a = accountRepo.findById(id).orElse(null);
		return new ResponseEntity<Account>(a, HttpStatus.OK);
	}
	
	@GetMapping("/email_verify")
	public ResponseEntity<Account> getAccount(@RequestParam("email") String email){
		Random rand = new Random();
		int code = rand.nextInt(9000) + 1000;
		boolean status = false;
		String password = "00000000";
		if(accountRepo.findAccountByEmail(email) == null) {
			Account accountUnverified = new Account(email, password, code, status);
			accountRepo.save(accountUnverified);
			notifService.sendVerification(email, code);
		}
		else {
			// throw exception: email already exist
		}
		return new ResponseEntity<Account>(HttpStatus.OK);
	}
	
	@PostMapping("/signup")
	public ResponseEntity<Account> saveVerifiedAccount(@RequestParam("email") String email,
													   @RequestParam("code") int code,
													   @RequestParam("password") String password){
		Account accountVerified = accountRepo.findAccountByEmail(email);
		if(accountVerified != null && code == accountVerified.getCode()) {
			accountVerified.setPassword(password);
			accountVerified.setStatus(true);
			accountRepo.save(accountVerified);
			notifService.sendNotification(email);
		}
		// to do redirect
		return new ResponseEntity<Account>(accountVerified,HttpStatus.OK);
	}

	/*login
	 * 
	@GetMapping("/login")
	public String login(@RequestParam("email") String email,
			   			@RequestParam("password") String password){
		Account a = accountRepo.findAccountByEmail(email);
		if(a != null && password == a.getPassword() && a.isStatus()) {
			return "redirect:/";
		}
		
		else if(a != null && password != a.getPassword()) {
			return "login";
		}
		
		return "signup";			
	}
	*/
}
