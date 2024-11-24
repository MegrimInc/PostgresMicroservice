package edu.help.microservice.service;

import java.util.List;
import java.util.Optional;

import edu.help.microservice.dto.CustomerNameRequest;
import edu.help.microservice.dto.CustomerNameResponse;
import edu.help.microservice.exception.CustomerNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.help.microservice.entity.Customer;
import edu.help.microservice.repository.CustomerRepository;

@Service
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    /**
     * Saves a Customer entity to the database.
     *
     * @param customer the Customer entity to save
     * @return the saved Customer entity
     */
    public Customer save(Customer customer) {
        return customerRepository.save(customer);
    }

    /**
     * Finds a Customer entity by its ID.
     *
     * @param id the ID of the Customer entity
     * @return an Optional containing the Customer entity if found, or empty if not
     */
    public Optional<Customer> findById(Integer id) {
        return customerRepository.findById(id);
    }

    /**
     * Retrieves all Customer entities.
     *
     * @return a list of all Customer entities
     */
    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    public void delete(Customer customer) {
        customerRepository.delete(customer);
    }
    

    /**
     * Deletes a Customer entity by its ID.
     *
     * @param id the ID of the Customer entity to delete
     */
    public void deleteById(Integer id) {
        customerRepository.deleteById(id);
    }

    /**
     * Finds a Customer by first name and last name.
     *
     * @param firstName the first name of the Customer
     * @param lastName  the last name of the Customer
     * @return the Customer entity if found, or null if not
     */
    public Customer findByFirstNameAndLastName(String firstName, String lastName) {
        return customerRepository.findByFirstNameAndLastName(firstName, lastName);
    }

    public String getName(Integer id) {
        var customerOptional = findById(id);
        return customerOptional.map(customer ->
                customer.getFirstName() + " " + customer.getLastName()
        ).orElse(null);
    }

    public CustomerNameResponse getNameData(int id) {
        var customerOptional = findById(id);
        if (customerOptional.isEmpty())
            throw new CustomerNotFoundException(id);

        Customer customer = customerOptional.get();
        return CustomerNameResponse.builder()
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .build();
    }

    public CustomerNameResponse updateNameData(CustomerNameRequest request, int id) {
        var customerOptional = findById(id);
        if (customerOptional.isEmpty())
            throw new CustomerNotFoundException(id);

        Customer customer = customerOptional.get();
        if (request.getFirstName() != null && !request.getFirstName().isEmpty())
            customer.setFirstName(request.getFirstName());
        if (request.getLastName() != null && !request.getLastName().isEmpty())
            customer.setLastName(request.getLastName());
        customerRepository.save(customer);

        return CustomerNameResponse.builder()
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .build();
    }
}
