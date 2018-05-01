package edu.sjsu.cmpe275.project.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.sjsu.cmpe275.project.entities.Survey;

public interface SurveyRepository extends JpaRepository<Survey, Integer>{
	public Survey findSurveyByLink(String link);

}
