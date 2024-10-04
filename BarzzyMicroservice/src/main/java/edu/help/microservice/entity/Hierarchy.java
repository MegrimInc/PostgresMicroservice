package edu.help.microservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;

@Entity
@NamedNativeQueries({
    @NamedNativeQuery(
        name = "Hierarchy.insertLtreePath",
        query = "INSERT INTO hierarchy (path, status, user_id, claimer) VALUES (CAST(? AS ltree), ?, ?, ?, ?)",
        resultClass = Hierarchy.class
    )
})
public class Hierarchy {

    @Id
    @Column(columnDefinition = "ltree")
    private String path;

    @Column
    private String status; 

    @Column(name = "user_id")
    private int userId;

    @Column
    private String claimer; // Default to NULL

    public Hierarchy() {}

    public Hierarchy(String path, String status, int userId, String claimer) {
        this.path = path;
        this.status = status;
        this.userId = userId;
        this.claimer = claimer;
    }

    // Getters and Setters
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }


    public String getClaimer() {
        return claimer;
    }

    public void setClaimer(String claimer) {
        this.claimer = claimer;
    }
}
