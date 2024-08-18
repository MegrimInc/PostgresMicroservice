package edu.help.microservice.controller;

import edu.help.microservice.dto.ResponseDTO;
import edu.help.microservice.dto.TagDTO;
import edu.help.microservice.dto.BarDTO;
import edu.help.microservice.entity.Drink;
import edu.help.microservice.service.BarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class BarController {

    @Autowired
    private BarService barService;

    @GetMapping("/bars/seeAll")
    public ResponseDTO seeAll() {
        return barService.findAllBarsAndTags();
    }

    
    @GetMapping("/bars/seeAllBars")
    public List<BarDTO> seeAllBars()
    {
        return barService.findAllBars();        
    }

    @GetMapping("/bars/seeAllTags")
    public List<TagDTO> seeAllTags()
    {
        return barService.findAllTags();
    }

    @GetMapping("/bars/getDrinks")
    public List<Drink> getDrinks(@RequestParam Integer categoryId, @RequestParam Integer barId) {
        return barService.findDrinksByCategoryIdAndBarId(categoryId, barId);
    }

    @GetMapping("/bars/seeAllDrinks")
    public List<Drink> seeAllDrinks()
    {
        return barService.findAllDrinks();
    }

    @GetMapping("/bars/getOneDrink")
    public Drink getOneDrink(@RequestParam Integer id)
    {
        return barService.findDrinkById(id);
    }

    @GetMapping("/bars/getSixDrinks")
    public List<Drink> getSixDrinks(@RequestParam Integer barId)
    {
        return barService.findDrinksByBarId(barId); 
    }
}
