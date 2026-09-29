package com.kkamar.cnh.repository;

import com.kkamar.cnh.domain.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {
    Optional<Device> findByDeviceUuid(String deviceUuid);
}