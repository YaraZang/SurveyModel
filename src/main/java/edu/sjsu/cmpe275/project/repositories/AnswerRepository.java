package edu.sjsu.cmpe275.project.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.sjsu.cmpe275.project.entities.Answer;

public interface AnswerRepository extends JpaRepository<Answer, Integer>{
	
	public Answer findAnswerByEmailAndSurveyId(String email, int surveyId);
	
	public Answer findAnswerBySurveyId(int surveyId);
}
