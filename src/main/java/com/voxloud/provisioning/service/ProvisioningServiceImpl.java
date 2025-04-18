package com.voxloud.provisioning.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voxloud.provisioning.entity.Device;
import com.voxloud.provisioning.entity.Device.DeviceModel;
import com.voxloud.provisioning.repository.DeviceRepository;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeMap;

/**
 * Implementation of ProvisioningService which provides provisioning data
 * based on device MAC address.
 * 
 * Retrieves default property values from Spring configuration and overrides
 * them using device-specific data stored in the database.
 */
@Service
public class ProvisioningServiceImpl implements ProvisioningService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Inject default values from application.properties with fallback defaults
    @Value("${spring.datasource.username:defaultUser}")
    private String propUsername;

    @Value("${spring.datasource.password:defaultPass}")
    private String propPassword;

    @Value("${provisioning.domain:com.default.domain}")
    private String propDomain;

    @Value("${provisioning.port:99}")
    private String propPort;

    @Value("${provisioning.codecs:OGG}")
    private String propCodecs;

    private final DeviceRepository deviceRepository;

    /**
     * Constructor for dependency injection.
     * 
     * @param deviceRepository repository to access Device entities
     */
    public ProvisioningServiceImpl(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    /**
     * Retrieve provisioning configuration for a device identified by its MAC address.
     * 
     * - Loads device entity from database by MAC address.
     * - Starts with default provisioning values from configuration.
     * - Overrides with device-specific username and password if present.
     * - Further overrides with any valid JSON or properties-format override fragment.
     * - Sorts the final configuration by keys ascending.
     * - Returns configuration as JSON string for CONFERENCE model devices.
     * - Returns configuration as Java properties format text for DESK model devices.
     * - Returns null if device model is unrecognized or serialization error occurs.
     * - Returns empty string if device is not found.
     * 
     * @param macAddress device MAC address (case-sensitive or insensitive depends on usage)
     * @return provisioning configuration string or empty string if device not found or null on error
     */
    @Override
    public String getProvisioningFile(String macAddress) {
        // Retrieve device by MAC address
        Optional<Device> deviceOpt = deviceRepository.findByMacAddress(macAddress);

        if (deviceOpt.isPresent()) {
            Device device = deviceOpt.get();

            // Initialize with default props
            Map<String, String> configData = new HashMap<>();
            configData.put("username", propUsername);
            configData.put("password", propPassword);
            configData.put("domain", propDomain);
            configData.put("port", propPort);
            configData.put("codecs", propCodecs);

            // Override with device username if non-empty
            String devUsername = device.getUsername();
            if (devUsername != null && !devUsername.isEmpty()) {
                configData.put("username", devUsername);
            }

            // Override with device password if non-empty
            String devPassword = device.getPassword();
            if (devPassword != null && !devPassword.isEmpty()) {
                configData.put("password", devPassword);
            }

            // Attempt to apply device override fragment if present
            String devOverride = device.getOverrideFragment();
            if (devOverride != null && !devOverride.isEmpty()) {
                try {
                    // Try parsing device override as JSON
                    JsonNode jsonNode = objectMapper.readTree(devOverride);
                    if (jsonNode.isObject()) {
                        // Override configData entries from JSON key-value pairs
                        jsonNode.fieldNames().forEachRemaining(key -> {
                            JsonNode valueNode = jsonNode.get(key);
                            if (valueNode.isValueNode()) {
                                configData.put(key, valueNode.asText());
                            }
                        });
                    }
                } catch (IOException jsonEx) {
                    // If JSON parse fails, attempt to parse as Java properties format
                    try {
                        Properties props = new Properties();
                        props.load(new StringReader(devOverride));
                        for (String key : props.stringPropertyNames()) {
                            configData.put(key, props.getProperty(key));
                        }
                    } catch (IOException propEx) {
                        // Log invalid override fragment format, but continue gracefully
                        propEx.printStackTrace();
                    }
                }
            }

            try {
                // Sort configuration keys for consistent output
                Map<String, String> configDataSorted = new TreeMap<>(configData);

                DeviceModel devModel = device.getModel();

                // Return JSON for CONFERENCE devices
                if (devModel == DeviceModel.CONFERENCE) {
                    return objectMapper.writeValueAsString(configDataSorted);
                } 
                // Return Java properties format for DESK devices
                else if (devModel == DeviceModel.DESK) {
                    StringBuilder propertiesString = new StringBuilder();
                    for (Map.Entry<String, String> entry : configDataSorted.entrySet()) {
                        propertiesString.append(entry.getKey())
                                        .append("=")
                                        .append(entry.getValue())
                                        .append("\n");
                    }
                    return propertiesString.toString();
                } 
                // Return null for unsupported device models
                else {
                    return null;
                }
            } catch (JsonProcessingException e) {
                // Log JSON processing errors and return null on failure
                e.printStackTrace();
                return null;
            }
        } else {
            // Return empty string if device not found
            return "";
        }
    }
}