package edu.sjsu.cmpe275.project.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.sjsu.cmpe275.project.entities.Invitation;


public interface InvitationRepository extends JpaRepository<Invitation, Integer>{

	public Invitation findInvitationByLink(String link);
}
