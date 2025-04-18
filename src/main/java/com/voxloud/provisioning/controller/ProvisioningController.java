package com.voxloud.provisioning.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voxloud.provisioning.service.ProvisioningService;

@RestController
@RequestMapping("/api/v1") // Base URI for all endpoints in this controller
public class ProvisioningController {

    private final ProvisioningService provisioningService;

    // Constructor injection for better testability and immutability
    @Autowired
    public ProvisioningController(ProvisioningService provisioningService) {
        this.provisioningService = provisioningService;
    }

    /**
     * GET endpoint to retrieve provisioning data by MAC address.
     * 
     * @param macAddress MAC address in path variable (case-insensitive)
     * @return Text containing JSON or Propery format provisioning data with HTTP 200 when found,
     *         HTTP 404 with error JSON if device not found,
     *         HTTP 500 with error JSON on unexpected errors
     */
    @GetMapping(value = "/provisioning/{macAddress}", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getDeviceByMacAddress(@PathVariable("macAddress") String macAddress) {
        // Normalize macAddress to lowercase for consistent matching
        macAddress = macAddress.toLowerCase();

        // Fetch provisioning JSON string from service
        String deviceConfig = provisioningService.getProvisioningFile(macAddress);

        // If service returns null or empty, handle error response
        if (deviceConfig == null || deviceConfig.isEmpty()) {
            Map<String, String> errorBody = new HashMap<>();

            // Distinguish between unexpected error (null) and device not found (empty)
            if (deviceConfig == null) {
                errorBody.put("error", "Unexpected error occured");
                errorBody.put("macAddress", macAddress);
            } else {
                errorBody.put("error", "Device not found");
                errorBody.put("macAddress", macAddress);
            }

            String errorBodyJson = "";
            try {
                // Convert error map to JSON string
                errorBodyJson = new ObjectMapper().writeValueAsString(errorBody);
            } catch (JsonProcessingException e) {
                // Log stack trace and return 500 with empty or partial JSON
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBodyJson);
            }

            if (deviceConfig == null) {
                // Unexpected error response
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBodyJson);
            } else {
                // Device not found response
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBodyJson);
            }
        } else {
            // Successful response with provisioning JSON
            return ResponseEntity.ok(deviceConfig);
        }
    }
}