package com.spring.ai_chatbot.repository;

import com.spring.ai_chatbot.entity.TravelPlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TravelPlanRepository extends JpaRepository<TravelPlanEntity, Long> {
    List<TravelPlanEntity> findTop10ByOrderByCreatedTimestampDesc();
    List<TravelPlanEntity> findByDestinationIgnoreCaseOrderByCreatedTimestampDesc(String destination);
}
