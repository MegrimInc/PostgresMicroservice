package edu.help.microservice.repository;

import edu.help.microservice.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer> {

    /**
     * Finds a Customer entity by first name and last name.
     *
     * @param firstName the first name of the Customer
     * @param lastName  the last name of the Customer
     * @return the Customer entity with the given first and last name, or null if not found
     */
    Customer findByFirstNameAndLastName(String firstName, String lastName);
}
