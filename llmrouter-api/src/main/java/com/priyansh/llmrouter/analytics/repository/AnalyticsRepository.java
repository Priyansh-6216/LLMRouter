package com.priyansh.llmrouter.analytics.repository;

import com.priyansh.llmrouter.analytics.entity.RequestAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalyticsRepository extends JpaRepository<RequestAnalytics, String> {
    List<RequestAnalytics> findByTenantId(String tenantId);
}
