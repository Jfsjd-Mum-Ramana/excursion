package com.verizon.ucs.restapi.controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.verizon.ucs.restapi.dto.AppApiResponse;
import com.verizon.ucs.restapi.dto.ApiRequest;
import com.verizon.ucs.restapi.model.Device;
import com.verizon.ucs.restapi.service.UCSPService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * Controller interface for UCS Portal Application with JPA
 */
@RestController
@RequestMapping("/portal")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080"})
public class UCSPController {
	private static Logger logger = LoggerFactory.getLogger(UCSPController.class);

	@Autowired
	private UCSPService uCSPService;

	@Operation(summary = "This operation is to get device details for the given device or IP or network or model or vendor")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "OK.") })
	@GetMapping("/get-devices")
	public ResponseEntity<?> getDevices(@ModelAttribute ApiRequest apiRequest) {

		AppApiResponse apiResponse = new AppApiResponse();
		try {
			List<Device> devices = uCSPService.searchDevices(apiRequest);
			if (devices == null || devices.isEmpty()) {
				apiResponse.setStatus(devices == null ? 400 : 204);
				apiResponse.setMessage(devices == null ? "Invalid search criteria." : "No matching devices found.");
			} else {
				apiResponse.setMessage("Search request successful.");
				apiResponse.setStatus(200);
			}
			apiResponse.setData(devices);
			return new ResponseEntity<>(apiResponse, HttpStatus.OK);

		} catch (Exception e) {
			String err = createErrorMessage("/get-devices", e);
			logger.error(err + " Detail Message: {}", e);
			apiResponse.setStatus(500);
			apiResponse.setMessage(e.getMessage());
			apiResponse.setError(err);
			return new ResponseEntity<>(apiResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private String createErrorMessage(String endpoint, Exception e) {
		return "Error: " + e.getMessage() + " In Class: " + e.getStackTrace()[0].getClassName()
				+ " In Method: " + e.getStackTrace()[0].getMethodName() + " at line number: "
				+ e.getStackTrace()[0].getLineNumber() + " at endpoint: " + endpoint;
	}
}
