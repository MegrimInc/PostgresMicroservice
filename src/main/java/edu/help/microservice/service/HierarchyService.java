package edu.help.microservice.service;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.help.microservice.repository.HierarchyRepository;

@Service
public class HierarchyService {

    @Autowired
    private HierarchyRepository hierarchyRepository;

    public void createHierarchy(int barId, int userId, String orderId, Set<Integer> drinkIds) {
        String formattedOrderId = orderId.replace("-", "_");
        String basePath = "root." + barId + "." + userId + "." + formattedOrderId;
        for (Integer drinkId : drinkIds) {
            String fullPath = basePath + "." + drinkId;
            hierarchyRepository.insertLtreePath(fullPath, 0, userId); // Use custom insert method with userId
        }
    }
}
