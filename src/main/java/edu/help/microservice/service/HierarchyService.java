package edu.help.microservice.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.help.microservice.dto.OrderToSave;
import edu.help.microservice.repository.HierarchyRepository;

@Service
public class HierarchyService {

    @Autowired
    private HierarchyRepository hierarchyRepository;


    @Transactional
    public void saveOrderToHierarchy(OrderToSave orderToSave) {
    String basePath = "root." + orderToSave.getBarId() + "." + orderToSave.getUserId() + "." + orderToSave.getTimestamp();

    for (OrderToSave.DrinkOrder drink : orderToSave.getDrinks()) {
        String path = basePath + "." + drink.getId();

        // Check if the path already exists in the hierarchy table
        Integer existingQuantity = hierarchyRepository.findQuantityByPath(path);

        if (existingQuantity != null) {
            // If the path exists, update the quantity
            int newQuantity = existingQuantity + drink.getQuantity();
            hierarchyRepository.updateQuantity(path, newQuantity);
        } else {
            // If the path does not exist, insert a new row
            hierarchyRepository.insertHierarchy(path, orderToSave.getStatus(), orderToSave.getUserId(), orderToSave.getClaimer(), drink.getQuantity());
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
            String fullPath = basePath + "." + drinkId + "." + quantity;
            
            // Check if the path already exists
            Integer currentRank = hierarchyRepository.findRankByPath(fullPath);
            int newRank = (currentRank != null) ? currentRank + 1 : 0;

            // Insert or update the record in the hierarchy table
            if (currentRank != null) {
                hierarchyRepository.updateRank(fullPath, newRank);
            } else {
                hierarchyRepository.insertLtreePath(fullPath, null, userId, newRank, null);
            }
        }
    }
}
