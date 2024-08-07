package edu.help.microservice.repository;

import edu.help.microservice.entity.UserData;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDataRepository extends JpaRepository<UserData, Long> {
    UserData findByEmail(String email);
}
