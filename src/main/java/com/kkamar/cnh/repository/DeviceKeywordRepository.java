package com.kkamar.cnh.repository;

import com.kkamar.cnh.domain.DeviceKeyword;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeviceKeywordRepository extends JpaRepository<DeviceKeyword, Long> {
    boolean existsByDeviceIdAndKeywordId(Long deviceId, Long keywordId);
    void deleteByDeviceIdAndKeywordId(Long deviceId, Long keywordId);
    List<DeviceKeyword> findByDeviceId(Long deviceId);
}