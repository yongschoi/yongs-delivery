package yongs.temp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import yongs.temp.dao.DeliveryRepository;
import yongs.temp.vo.Order;

@Service
public class DeliveryService {
	private static final Logger logger = LoggerFactory.getLogger(DeliveryService.class);
	// for sender
	private static final String DELIVERY_ORDER_EVT = "delivery-to-order";
	private static final String DELIVERY_ROLLBACK_EVT = "delivery-rollback";
	
	// for listener
	private static final String PAYMENT_DELIVERY_EVT = "payment-to-delivery";
	
	@Autowired
    KafkaTemplate<String, String> kafkaTemplate;
	
	@Autowired
    DeliveryRepository repo;

	@KafkaListener(topics = PAYMENT_DELIVERY_EVT)
	public void create(String orderStr) throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		Order order = mapper.readValue(orderStr, Order.class);
		
		long curr = System.currentTimeMillis();
		String no = "DEL" + curr;
		order.getDelivery().setNo(no);
		order.getDelivery().setOpentime(curr);
		order.getDelivery().setOrderNo(order.getNo());
		
		try {
			// Delivery API call
			// ...
			// ...
			repo.insert(order.getDelivery());
			String newOrderStr = mapper.writeValueAsString(order);
			logger.info(">>>>> DELIVERY 성공  >>>>>> " + order.getNo());
			kafkaTemplate.send(DELIVERY_ORDER_EVT, newOrderStr);
		} catch (Exception e) {
			logger.info(">>>>> DELIVERY 실패  >>>>>> " + order.getNo());
			kafkaTemplate.send(DELIVERY_ROLLBACK_EVT, orderStr);			
		}
	}
}
