package edu.help.microservice.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.help.microservice.dto.BarDTO;
import edu.help.microservice.dto.ResponseDTO;
import edu.help.microservice.dto.TagDTO;
import edu.help.microservice.entity.Bar;
import edu.help.microservice.entity.Drink;
import edu.help.microservice.entity.Tag;
import edu.help.microservice.repository.BarRepository;
import edu.help.microservice.repository.DrinkRepository;
import edu.help.microservice.repository.TagRepository;
import edu.help.microservice.util.DTOConverter;

@Service
public class BarService {

    @Autowired
    private BarRepository barRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private DrinkRepository drinkRepository;

    public List<BarDTO> findAllBars()
    {
        List<Bar> bars = barRepository.findAll();
        List<BarDTO> barDTOs = bars.stream().map(DTOConverter::convertToBarDTO).collect(Collectors.toList());
        return barDTOs;
    }

    public List<TagDTO> findAllTags()
    {
        List<Tag> tags = tagRepository.findByCategoryPathPattern();
        List<TagDTO> tagDTOs = tags.stream().map(DTOConverter::convertToTagDTO).collect(Collectors.toList());
        return tagDTOs;
    }

    public List<Drink> findAllDrinks()
    {
        return drinkRepository.findAll();
    }


    public ResponseDTO findAllBarsAndTags() {
        List<Bar> bars = barRepository.findAll();
        List<Tag> tags = tagRepository.findByCategoryPathPattern();

        List<BarDTO> barDTOs = bars.stream().map(DTOConverter::convertToBarDTO).collect(Collectors.toList());
        List<TagDTO> tagDTOs = tags.stream().map(DTOConverter::convertToTagDTO).collect(Collectors.toList());

        ResponseDTO responseDTO = new ResponseDTO();
        responseDTO.setBars(barDTOs);
        responseDTO.setTags(tagDTOs);

        return responseDTO;
    }

    public List<Drink> findDrinksByCategoryIdAndBarId(Integer categoryId, Integer barId) {
        List<Integer> drinkIds = tagRepository.findDrinkIdsByCategoryIdAndBarId(categoryId, barId);
        return drinkRepository.findByDrinkIdIn(drinkIds);
    }

    public Drink findDrinkById(Integer drinkId) {
        return drinkRepository.findById(drinkId).orElse(null);
    }

    public Bar findByBarEmail(String bar_email) {
        return barRepository.findByBarEmail(bar_email).orElse(null);
    }

}
