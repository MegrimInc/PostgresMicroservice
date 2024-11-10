package edu.help.microservice.repository;

import edu.help.microservice.entity.Bar;

import edu.help.microservice.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Integer> {


}
