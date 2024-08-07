package edu.help.microservice.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "Registration")
public class Registration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RegistrationID")
    private Long registrationId;

    @Column(name = "Email", unique = true, nullable = false)
    private String email;

    @Column(name = "BarID")
    private Integer barId;

    @Column(name = "UserID")
    private Long userId;  // Corrected to Long

    @Column(name = "Password", nullable = false)
    private String password;

    // Getters and setters
    public Long getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId(Long registrationId) {
        this.registrationId = registrationId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getBarId() {
        return barId;
    }

    public void setBarId(Integer barId) {
        this.barId = barId;
    }

    public Long getUserId() {  // Corrected getter
        return userId;
    }

    public void setUserId(Long userId) {  // Corrected setter
        this.userId = userId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
