package edu.help.microservice.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.help.microservice.repository.HierarchyRepository;

@Service
public class HierarchyService {

    @Autowired
    private HierarchyRepository hierarchyRepository;

    @Transactional
    public void createHierarchy(int barId, int userId, String orderId, Map<Integer, Integer> drinkQuantities) {
        String formattedOrderId = orderId.replace("-", "_");
        String basePath = "root." + barId + "." + userId + "." + formattedOrderId;
        
        for (Map.Entry<Integer, Integer> entry : drinkQuantities.entrySet()) {
            Integer drinkId = entry.getKey();
            Integer quantity = entry.getValue();

            // Create the full path
            String fullPath = basePath + "." + drinkId + "." + quantity;
            
            // Check if the path already exists
            Integer currentRank = hierarchyRepository.findRankByPath(fullPath);
            int newRank = (currentRank != null) ? currentRank + 1 : 0;

            // Insert or update the record in the hierarchy table
            if (currentRank != null) {
                hierarchyRepository.updateRank(fullPath, newRank);
            } else {
                hierarchyRepository.insertLtreePath(fullPath, 0, userId, newRank, null);
            }
        }
    }
}
