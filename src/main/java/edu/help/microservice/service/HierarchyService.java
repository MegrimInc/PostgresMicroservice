package edu.help.microservice.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.help.microservice.dto.OrderToSave;
import edu.help.microservice.entity.Hierarchy;
import edu.help.microservice.repository.HierarchyRepository;

@Service
public class HierarchyService {

    @Autowired
    private HierarchyRepository hierarchyRepository;

    @Transactional
    public void saveOrderToHierarchy(OrderToSave orderToSave) {
        String basePath = "root." + orderToSave.getBarId() + "." + orderToSave.getUserId() + "." + orderToSave.getTimestamp();
        
        for (OrderToSave.DrinkOrder drink : orderToSave.getDrinks()) {
            String pathWithoutQuantity = basePath + "." + drink.getId();
            
            // Log the path we're working with
            System.out.println("Checking path: " + pathWithoutQuantity);
            
            try {
                // Check if a path for this drink already exists
                Integer existingQuantity = hierarchyRepository.findQuantityByPath(pathWithoutQuantity);
                
                if (existingQuantity != null) {
                    // If the path exists, update the quantity
                    int newQuantity = existingQuantity + drink.getQuantity();
                    String newPath = pathWithoutQuantity + "." + newQuantity;
                    System.out.println("Updating path: " + newPath);
                    hierarchyRepository.updateQuantityForSaveOrder(pathWithoutQuantity, newPath);
                } else {
                    // If the path does not exist, insert a new row with the initial quantity
                    String fullPath = pathWithoutQuantity + "." + drink.getQuantity();
                    System.out.println("Inserting new path: " + fullPath);
                    hierarchyRepository.insertLtreePath2(fullPath, orderToSave.getStatus(), orderToSave.getUserId(), 0, orderToSave.getClaimer());
                }
            } catch (Exception e) {
                System.err.println("Error processing path: " + pathWithoutQuantity);
                e.printStackTrace();
                throw e;  // Re-throw the exception after logging
            }
        }
    }


    @Transactional
    public void createHierarchy(int barId, int userId, String orderId, Map<Integer, Integer> drinkQuantities) {
        String formattedOrderId = orderId.replace("-", "_");
        String basePath = "root." + barId + "." + userId + "." + formattedOrderId;
        
        for (Map.Entry<Integer, Integer> entry : drinkQuantities.entrySet()) {
            Integer drinkId = entry.getKey();
            Integer quantity = entry.getValue();

            // Create the full path
            String fullPath = basePath + "." + drinkId;
            
            // Check if the path already exists
            Integer currentRank = hierarchyRepository.findRankForCreateHierarchy(fullPath);
            int newRank = (currentRank != null) ? currentRank + 1 : 0;

            // Insert or update the record in the hierarchy table
            if (currentRank != null) {
                hierarchyRepository.updateRankForCreateHierarchy(fullPath, newRank);
            } else {
                hierarchyRepository.insertLtreePathForCreateHierarchy(fullPath, null, userId, newRank, null);
            }
        }
    }
}
