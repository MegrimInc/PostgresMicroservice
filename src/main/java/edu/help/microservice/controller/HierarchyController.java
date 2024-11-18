package edu.help.microservice.controller;

import java.util.Map; // Correct import statement
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.help.microservice.dto.OrderToSave;
import edu.help.microservice.service.HierarchyService;

@RestController
@RequestMapping("/hierarchy")
public class HierarchyController {

    @Autowired
    private HierarchyService hierarchyService;


    @PostMapping("/save")
    public String saveOrder(@RequestBody OrderToSave orderToSave) {
        System.out.println("....BeforeSavingOrder...");
        hierarchyService.saveOrderToHierarchy(orderToSave);
        System.out.println("...AfterSavingOrder...");
        
        return "Order saved to hierarchy successfully.";
    }


    public static class HierarchyRequest {
        private int barId;
        private int userId;
        private String orderId;
        private Map<Integer, Integer> drinkQuantities; // Change to Map

        // Getters and Setters
        public int getBarId() {
            return barId;
        }

        public void setBarId(int barId) {
            this.barId = barId;
        }

        public int getUserId() {
            return userId;
        }

        public void setUserId(int userId) {
            this.userId = userId;
        }

        public String getOrderId() {
            return orderId;
        }

        public void setOrderId(String orderId) {
            this.orderId = orderId;
        }

        public Map<Integer, Integer> getDrinkQuantities() { // Getter for Map
            return drinkQuantities;
        }

        public void setDrinkQuantities(Map<Integer, Integer> drinkQuantities) { // Setter for Map
            this.drinkQuantities = drinkQuantities;
        }
    }
}
