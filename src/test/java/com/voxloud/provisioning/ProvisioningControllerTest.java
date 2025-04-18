package com.voxloud.provisioning;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest
@AutoConfigureMockMvc  // Enables and configures MockMvc for integration testing of MVC controllers
public class ProvisioningControllerTest {

    @Autowired
    private MockMvc mockMvc;  // MockMvc to simulate HTTP requests without starting a server

    // Logger instance for better logging and diagnostics
    private static final Logger logger = LoggerFactory.getLogger(ProvisioningControllerTest.class);

    /**
     * Integration test for the provisioning endpoint with multiple MAC addresses.
     * Checks for correct HTTP status codes and minimal response content validation.
     */
    @Test
    public void testProvisioningEndpoints() throws Exception {
        String[] macAddresses = {
            "aa-bb-cc-dd-ee-ff",
            "f1-e2-d3-c4-b5-a6",
            "a1-b2-c3-d4-e5-f6",
            "1a-2b-3c-4d-5e-6f",
            "aa-aa-aa-aa-aa-aa"
        };

        for (String mac : macAddresses) {
            String path = "/api/v1/provisioning/" + mac;

            MvcResult result = mockMvc.perform(get(path)
                                    .accept("text/plain"))
                                    .andReturn();

            int status = result.getResponse().getStatus();
            String response = result.getResponse().getContentAsString();

            if (status == 200) {
                // Assert that the response body is not empty for found devices
                assertThat("Response should contain expected device data for mac " + mac, response, not(isEmptyString()));
            } else if (status == 404) {
                // Assert error JSON contains "error" field for not found device
                assertThat("Error response should include 'error' field", response, containsString("error"));
            } else {
                // Fail the test on unexpected HTTP status
                fail("Unexpected HTTP status " + status + " for mac " + mac);
            }
        }
    }

    /**
     * Integration test that verifies expected exact JSON responses from provisioning endpoint.
     * Provides explicit expected JSON string responses for defined MAC addresses.
     */
    @Test
    public void testProvisioningEndpointsResult() throws Exception {
        // Map of MAC addresses to their expected exact JSON / Property format results
        Map<String, String> macMap = new HashMap<>();

        macMap.put("aa-bb-cc-dd-ee-ff", "codecs=G711,G729,OPUS\n" + 
                    "domain=sip.voxloud.com\n" + 
                    "password=doe\n" + 
                    "port=5060\n" + 
                    "username=john\n");
        macMap.put("f1-e2-d3-c4-b5-a6", "{\"codecs\":\"G711,G729,OPUS\",\"domain\":\"sip.voxloud.com\",\"password\":\"red\",\"port\":\"5060\",\"username\":\"sofia\"}");
        macMap.put("a1-b2-c3-d4-e5-f6", "codecs=G711,G729,OPUS\n" + 
                    "domain=sip.anotherdomain.com\n" + 
                    "password=white\n" + 
                    "port=5161\n" + 
                    "timeout=10\n" +
                    "username=walter\n");
        macMap.put("1a-2b-3c-4d-5e-6f", "{\"codecs\":\"G711,G729,OPUS\",\"domain\":\"sip.anotherdomain.com\",\"password\":\"blue\",\"port\":\"5161\",\"timeout\":\"10\",\"username\":\"eric\"}");
        macMap.put("aa-aa-aa-aa-aa-aa", "{\"macAddress\":\"aa-aa-aa-aa-aa-aa\",\"error\":\"Device not found\"}");

        for (Map.Entry<String, String> entry : macMap.entrySet()) {
            String mac = entry.getKey();
            String expectedResponse = entry.getValue();
            String path = "/api/v1/provisioning/" + mac;

            MvcResult result = mockMvc.perform(get(path)
                                    .accept("text/plain"))
                                    .andReturn();

            int status = result.getResponse().getStatus();
            String response = result.getResponse().getContentAsString();

            logger.info("TEST = {}={} => {}", mac, expectedResponse, response);

            if (status == 200) {
                // Use exact match for expected successful device data JSON
                assertThat("Response should match expected device data for mac " + mac, response, is(expectedResponse));
            } else if (status == 404) {
                // Error response should include 'error' field
                assertThat("Error response should include 'error' field", response, containsString("error"));
            } else {
                fail("Unexpected HTTP status " + status + " for mac " + mac);
            }
        }
    } 

    // WebApplicationContext autowired to gain access to Spring MVC handler mappings
    @Autowired
    private WebApplicationContext wac;

    /**
     * Utility test method to log all registered request handler mappings in the Spring context.
     * Useful for debugging and verifying available API routes.
     */
    @Test
    public void printAllHandlers() {
        RequestMappingHandlerMapping handlerMapping = wac.getBean(RequestMappingHandlerMapping.class);
        handlerMapping.getHandlerMethods().forEach((key, value) -> {
            logger.info("KEY = {} => {}", key, value);
        });
    }

}