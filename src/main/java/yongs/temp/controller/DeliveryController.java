package yongs.temp.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import yongs.temp.model.Delivery;
import yongs.temp.service.DeliveryService;

@RestController
@RequestMapping("/delivery")
public class DeliveryController {
    private static final Logger logger = LoggerFactory.getLogger(DeliveryController.class);	

    @Autowired
    DeliveryService service;
    
    @PutMapping("/update") 
    public void update(@RequestBody Delivery delivery) throws Exception{
    	logger.debug("yongs-delivery|DeliveryController|update({})", delivery.getOrderNo());
        service.updateDelivery(delivery);
    }  
}