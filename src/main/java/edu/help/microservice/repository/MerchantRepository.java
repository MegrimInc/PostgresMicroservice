package edu.help.microservice.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import edu.help.microservice.entity.Merchant;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, Integer> {

}