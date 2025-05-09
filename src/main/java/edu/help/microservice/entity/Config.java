package edu.help.microservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Config {

    @Id
    @Column(name = "key")
    private String key;

    @Column(name = "value")
    private String value;
}