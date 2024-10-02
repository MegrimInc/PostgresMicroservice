package edu.help.microservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Entity
@Table(name = "registration")
@Getter
@Setter
public class Registration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer registrationID;

    @Column(nullable = false, unique = true)
    private String email;

    private String passcode;

    @Column(name = "is_bar")
    private Boolean isBar = false;

    @ManyToOne
    @JoinColumn(name = "user_dataid", referencedColumnName = "userID", nullable = true)
    private UserData userData;
}

