package com.voxloud.provisioning.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.voxloud.provisioning.entity.Device;

import java.util.Optional;

/**
 * Repository interface for Device entities.
 * 
 * Extends JpaRepository to provide standard CRUD operations.
 * The entity ID type is String, assumed to be the MAC address or another unique identifier.
 */
@Repository
public interface DeviceRepository extends JpaRepository<Device, String> {

    /**
     * Finds a Device entity by its MAC address.
     * 
     * @param macAddress the MAC address of the device
     * @return an Optional containing the Device if found, or empty if not found
     */
    Optional<Device> findByMacAddress(String macAddress);

}