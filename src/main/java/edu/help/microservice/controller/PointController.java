package edu.help.microservice.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.help.microservice.service.PointService;

@RestController
@RequestMapping("/points")
public class PointController {

    @Autowired
    private PointService pointService;

    // Retrieve points for a given user
    @GetMapping("/{userId}")
    public ResponseEntity<Map<Integer, Map<Integer, Integer>>> getPointsForUser(@PathVariable int userId) {
        Map<Integer, Map<Integer, Integer>> points = pointService.getPointsForUser(userId);
        return ResponseEntity.ok(points);
    }
}