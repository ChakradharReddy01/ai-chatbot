package com.spring.ai_chatbot.repository;

import com.spring.ai_chatbot.entity.WeatherLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WeatherLogRepository extends JpaRepository<WeatherLogEntity, Long> {
    List<WeatherLogEntity> findTop15ByOrderByQueryTimestampDesc();
    List<WeatherLogEntity> findByCityIgnoreCaseOrderByQueryTimestampDesc(String city);
}
