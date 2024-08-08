package edu.help.microservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;

@Entity
@NamedNativeQueries({
    @NamedNativeQuery(
        name = "Hierarchy.insertLtreePath",
        query = "INSERT INTO hierarchy (path, status, user_id) VALUES (CAST(? AS ltree), ?, ?)",
        resultClass = Hierarchy.class
    )
})
public class Hierarchy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "ltree")
    private String path;

    @Column
    private int status = 0; // Default to 0

    @Column(name = "user_id")
    private int userId;

    public Hierarchy() {}

    public Hierarchy(String path, int status, int userId) {
        this.path = path;
        this.status = status;
        this.userId = userId;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }
}
