package edu.help.microservice.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "registration")
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

    // Getters and Setters

    public Integer getRegistrationID() {
        return registrationID;
    }

    public void setRegistrationID(Integer registrationID) {
        this.registrationID = registrationID;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasscode() {
        return passcode;
    }

    public void setPasscode(String passcode) {
        this.passcode = passcode;
    }

    public Boolean getIsBar() {
        return isBar;
    }

    public void setIsBar(Boolean isBar) {
        this.isBar = isBar;
    }

    public UserData getUserData() {
        return userData;
    }

    public void setUserData(UserData userData) {
        this.userData = userData;
    }
}

