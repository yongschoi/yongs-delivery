package yongs.temp.dao;

import org.springframework.data.mongodb.repository.MongoRepository;

import yongs.temp.model.Delivery;

public interface DeliveryRepository extends MongoRepository<Delivery, String> {
	public void deleteByOrderNo(final String orderNo);
}