package edu.help.microservice.entity;

import java.util.Map;

import org.hibernate.annotations.Type;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "customer")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer customerID;

    @Column(name = "acceptedtos", nullable = false)
    private Boolean acceptedTOS = false;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @OneToOne(mappedBy = "customer", cascade = CascadeType.ALL)
    private SignUp signUp;

    @Type(JsonType.class)
    @Column(name = "points", columnDefinition = "jsonb")
    private Map<Integer, Map<Integer, Integer>> points;

    // Getters and Setters
    public Integer getCustomerID() {
        return customerID;
    }

    public void setCustomerID(Integer customerID) {
        this.customerID = customerID;
    }

    public Boolean getAcceptedTOS() {
        return acceptedTOS;
    }

    public void setAcceptedTOS(Boolean acceptedTOS) {
        this.acceptedTOS = acceptedTOS;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public SignUp getSignUp() {
        return signUp;
    }

    public void setSignUp(SignUp signUp) {
        this.signUp = signUp;
    }

    public Map<Integer, Map<Integer, Integer>> getPoints() {
        return points;
    }

    public void setPoints(Map<Integer, Map<Integer, Integer>> points) {
        this.points = points;
    }
}