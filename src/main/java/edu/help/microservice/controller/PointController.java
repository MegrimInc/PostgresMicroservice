package edu.help.microservice.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    // Add points for a specific user and bar ID
    @PostMapping("/add")
    public ResponseEntity<String> addPoints(
            @RequestParam int userId,
            @RequestParam int barId,
            @RequestParam int points) {
        pointService.addPoints(userId, barId, points);
        return ResponseEntity.ok("Points added successfully.");
    }

    // Subtract points for a specific user and bar ID
    @PostMapping("/subtract")
    public ResponseEntity<String> subtractPoints(
            @RequestParam int userId,
            @RequestParam int barId,
            @RequestParam int points) {
        pointService.subtractPoints(userId, barId, points);
        return ResponseEntity.ok("Points subtracted successfully.");
    }
}