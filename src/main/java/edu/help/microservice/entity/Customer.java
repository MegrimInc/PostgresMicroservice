package edu.help.microservice.entity;

import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

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

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    @ToString.Exclude 
    @OneToOne(mappedBy = "customer", cascade = CascadeType.ALL)
    private SignUp signUp;

    @Column(name = "stripe_id", nullable = true)
    private String stripeId;

    @Type(JsonType.class)
    @Column(name = "points", columnDefinition = "jsonb")
    private Map<Integer, Map<Integer, Integer>> points = new HashMap<>();

    @Column(name = "payment_id", nullable = true)
    private String paymentId;

    @Type(JsonType.class)
    @Column(name = "subscription", columnDefinition = "jsonb")
    private Map<Integer, SubscriptionInfo> subscription = new HashMap<>();
}