package edu.help.microservice.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.help.microservice.dto.OrderToSave;
import edu.help.microservice.dto.OrderToSave.DrinkOrder;
import edu.help.microservice.repository.HierarchyRepository;

@Service
public class HierarchyService {

    @Autowired
    private HierarchyRepository hierarchyRepository;

    @Transactional
    public void saveOrderToHierarchy(OrderToSave order) {
        Map<Integer, Integer> drinkQuantityMap = new HashMap<>();

        System.out.println("...In.Service.Made.The.Map...");


        for (DrinkOrder drinkOrder : order.getDrinks()) {
            drinkQuantityMap.put(drinkOrder.getId(), drinkOrder.getQuantity());
            System.out.println("...PopulatingMap...");
        }

        System.out.println("...CallingCreateHierarchy...");
        createHierarchy(order.getBarId(), order.getUserId(), order.getTimestamp(), 
            drinkQuantityMap, order.getStatus(), order.getClaimer());
        System.out.println("...JustCalledCreateHierarchy...");
    }

    
    @Transactional
    public void createHierarchy(int barId, int userId, String timestamp, Map<Integer, Integer> drinkQuantities, String status, String claimer) {
        String basePath = "root." + barId + "." + userId + "." + timestamp;

        for (Map.Entry<Integer, Integer> entry : drinkQuantities.entrySet()) {
            Integer drinkId = entry.getKey();
            Integer quantity = entry.getValue();

            // Create the full path
            String fullPath = basePath + "." + drinkId + "." + quantity;
            System.out.println("...Calling repository...");
            hierarchyRepository.insertLtreePathForCreateHierarchy(fullPath, status, userId, claimer);
        }
    }
}
