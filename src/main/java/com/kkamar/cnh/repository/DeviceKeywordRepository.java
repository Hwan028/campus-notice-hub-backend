package com.kkamar.cnh.repository;

import com.kkamar.cnh.domain.DeviceKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DeviceKeywordRepository extends JpaRepository<DeviceKeyword, Long> {
    boolean existsByDeviceIdAndKeywordId(Long deviceId, Long keywordId);
    void deleteByDeviceIdAndKeywordId(Long deviceId, Long keywordId);
    List<DeviceKeyword> findByDeviceId(Long deviceId);

    /** 피드 필터용: 연관 엔티티 없이 키워드 이름만 바로 가져온다. */
    @Query("select k.name from DeviceKeyword dk join dk.keyword k where dk.device.id = :deviceId")
    List<String> findKeywordNamesByDeviceId(@Param("deviceId") Long deviceId);
}
