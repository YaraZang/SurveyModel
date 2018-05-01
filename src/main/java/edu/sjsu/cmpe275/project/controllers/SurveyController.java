package edu.sjsu.cmpe275.project.controllers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.annotation.JsonView;

import edu.sjsu.cmpe275.project.View;
import edu.sjsu.cmpe275.project.entities.Account;
import edu.sjsu.cmpe275.project.entities.Invitation;
import edu.sjsu.cmpe275.project.entities.Question;
import edu.sjsu.cmpe275.project.entities.Survey;
import edu.sjsu.cmpe275.project.entities.Survey.SURVEY_TYPES;
import edu.sjsu.cmpe275.project.repositories.AccountRepository;
import edu.sjsu.cmpe275.project.repositories.InvitationRepository;
import edu.sjsu.cmpe275.project.repositories.QuestionRepository;
import edu.sjsu.cmpe275.project.repositories.SurveyRepository;
import edu.sjsu.cmpe275.project.services.NotificationService;

@Controller
public class SurveyController {
	
	@Autowired
	private SurveyRepository surveyRepo;
	
	@Autowired
	private AccountRepository accountRepo;
	
	@Autowired
	private QuestionRepository questionRepo;
	
	@Autowired
	private NotificationService notifService;
	
	@Autowired
	private InvitationRepository invitationRepo;
	
	
	//get all surveys of an account
	@GetMapping(value="/account/{accountId}/allsurveys")
	@JsonView(View.Survey.class)
	public ResponseEntity<List<Survey>> getOneSurvey(@PathVariable("accountId") int accountId){
		List<Survey> surveys = new ArrayList<>();
		if(accountRepo.findById(accountId).orElse(null) != null) {
			surveyRepo.findAll().forEach(surveys::add);
		}
		return new ResponseEntity<List<Survey>>(surveys, HttpStatus.OK);
	}
	
	//get one survey according to surveyId of an account
	@GetMapping(value="/account/{accountId}/survey")
	@JsonView(View.Survey.class)
	public ResponseEntity<Survey> getOneSurvey(@PathVariable("accountId") int accountId, @RequestParam("surveyId") int surveyId){
		Survey s = new Survey();
		if(accountRepo.findById(accountId).orElse(null) != null) {
			s = surveyRepo.findById(surveyId).orElse(null);
		}
		return new ResponseEntity<Survey>(s, HttpStatus.OK);
	}
	
	//create a survey
	@PostMapping(value="/account/{accountId}/survey")
	@JsonView(View.Survey.class)
	public ResponseEntity<Survey> createSurvey(@RequestBody Survey survey, @PathVariable("accountId") int accountId){
		Account account = accountRepo.findById(accountId).orElse(null);
		survey.setAccount(account);
		account.addSurvey(survey);
		for(Question q: survey.getQuestions()) {
			questionRepo.save(q);
			q.setSurvey(survey);
		}

		String link = "";
		String uuid = UUID.randomUUID().toString().replace("-", "");
		
		switch(survey.getSurveyType()) {
		case CLOSED_INVITATION:
			link = "";
			break;
			default:
				link = uuid;	
		}

		survey.setLink(link);
		survey.setUpdateTime(new Date());
		surveyRepo.save(survey);
		//redirect "/account/{accountId}/survey/{surveyId}/invitation"
		return new ResponseEntity<Survey>(survey, HttpStatus.OK);
	}

	//update survey
	@JsonView(View.Survey.class)
	@PutMapping(value="/account/{accountId}/survey/{surveyId}")
	public ResponseEntity<Survey> editSurvey(@RequestBody Survey survey, 
											 @PathVariable("accountId") int accountId,
											 @PathVariable("surveyId") int surveyId){
		Survey s = surveyRepo.findById(surveyId).orElse(null);
		s.setSurveyType(survey.getSurveyType());
		s.setStartTime(survey.getStartTime());
		s.setEndTime(survey.getEndTime());
		s.setQuestions(survey.getQuestions());
		s.setUpdateTime(new Date());
		s.setQuestions(survey.getQuestions());
		for(Question q: survey.getQuestions()) {
			questionRepo.save(q);
			q.setSurvey(s);
		}
		
		surveyRepo.save(s);
		return new ResponseEntity<Survey>(s, HttpStatus.OK);
	}

	//create link and send invitation email
	@JsonView(View.Survey.class)
	@PostMapping(value="/account/{accountId}/survey/{surveyId}/invitation")
	public ResponseEntity<Survey> sendInvitation(@PathVariable("accountId") int accountId,
												 @PathVariable("surveyId") int surveyId,
												 @RequestParam(value = "emails", required=false, defaultValue="") String emailArrayStr){
		Survey s = surveyRepo.findById(surveyId).orElse(null);
		SURVEY_TYPES surveyType = s.getSurveyType();
		String link = s.getLink();
		String[] emailArray = emailArrayStr.split(","); //suppose ","
		ArrayList<String> emailList = new ArrayList<String>(Arrays.asList(emailArray));
		
		for(String email : emailList) {
			switch(surveyType) {
				case CLOSED_INVITATION:
					String uuid = UUID.randomUUID().toString().replace("-", "");
					link = uuid;
					Invitation invitation = new Invitation(link, email, s);
					invitationRepo.save(invitation);
				default:
					notifService.sendInvitation(email, link);
			}
		}
		
		return new ResponseEntity<Survey>(HttpStatus.OK);
	}
	
	@JsonView(View.Survey.class)
	@GetMapping(value = "/survey/{uuid}")
	public ResponseEntity<Survey> getSurveyeeSurvey(@PathVariable("uuid") String uuid){
		
		Survey s = surveyRepo.findSurveyByLink(uuid);
		//type check
		if(s == null) {
			Invitation invitation = invitationRepo.findInvitationByLink(uuid);
			s = invitation.getSurvey();
		}
		//check date if it is expired
		Date currentDate = new Date();
		if(s.getEndTime().before(currentDate)) {
			return new ResponseEntity<Survey>(HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<Survey>(s, HttpStatus.OK);
	}
}
