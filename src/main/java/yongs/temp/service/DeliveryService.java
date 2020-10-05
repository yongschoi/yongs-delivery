package yongs.temp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import yongs.temp.dao.DeliveryRepository;
import yongs.temp.model.Delivery;
import yongs.temp.vo.Order;

@Service
public class DeliveryService {
	// status 0:주문/결제완료, 1:상품준비, 2:배송중, 3:배송완료 
	private static final Logger logger = LoggerFactory.getLogger(DeliveryService.class);
	// for sender
	private static final String DELIVERY_ORDER_EVT = "delivery-to-order";
	private static final String DELIVERY_ROLLBACK_EVT = "delivery-rollback";
	private static final String DELIVERY_UPDATE_EVT = "deliveryUpdate";
	
	// for listener
	private static final String PAYMENT_DELIVERY_EVT = "payment-to-delivery";
	
	@Autowired
    KafkaTemplate<String, String> kafkaTemplate;
	
	@Autowired
    DeliveryRepository repo;

	@KafkaListener(topics = PAYMENT_DELIVERY_EVT)
	public void create(String orderStr, Acknowledgment ack) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			Order order = mapper.readValue(orderStr, Order.class);
			// Delivery에 order no만 셋팅하고 저장
			order.getDelivery().setOrderNo(order.getNo());
		
			// Delivery API call
			// ...
			// ...
			// throw new Exception();
			repo.insert(order.getDelivery());
			String newOrderStr = mapper.writeValueAsString(order);
			kafkaTemplate.send(DELIVERY_ORDER_EVT, newOrderStr);
			logger.debug("[DELIVERY to ORDER(배송성공)] Order No [" + order.getNo() + "]");
		} catch (Exception e) {
			kafkaTemplate.send(DELIVERY_ROLLBACK_EVT, orderStr);	
			logger.debug("[DELIVERY Exception(배송 실패)]");
		}
		// 성공하든 실패하든 상관없이
		ack.acknowledge();
	}
	
	// 최초 생성된 Delivery에 선정된 배송업체를 저장한다.(실질적인 Delivery data생성)
	public void updateDelivery(Delivery delivery) throws JsonProcessingException {
		Delivery savedDelivery = repo.findByOrderNo(delivery.getOrderNo());
		logger.debug("delivery.getOrderNo( => " + delivery.getOrderNo());
		logger.debug("savedDelivery => " + savedDelivery);
		logger.debug("savedDelivery.getType() => " + savedDelivery.getType());
			
		long curr = System.currentTimeMillis();
		String no = "DEL" + curr;
		savedDelivery.setNo(no);
		savedDelivery.setOpentime(curr);		
		savedDelivery.setCompany(delivery.getCompany());
		
		// Delivery에 저장하고 
		repo.save(savedDelivery);
		
		// 데이타 변경되었으므로 event 발생
		ObjectMapper mapper = new ObjectMapper();
		String savedDeliveryStr = mapper.writeValueAsString(savedDelivery);
		// data 일관성 유지를 위한 event에는 rollback이 존재하는게 이상함.
		kafkaTemplate.send(DELIVERY_UPDATE_EVT, savedDeliveryStr);
		logger.debug("[DELIVERY update broadcasting] Delivery No [" + savedDelivery.getNo() + "]");
	}
}
