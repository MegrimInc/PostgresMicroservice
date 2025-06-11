package edu.help.microservice.repository;

import edu.help.microservice.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByCustomerIdAndMerchantIdOrderByCreatedAt(Integer customerId, Integer merchantId);
}
