import { Box, Button, Card, CardContent, Container, FormControl, Grid, InputLabel, MenuItem, Select, SelectChangeEvent, TextField, Typography } from '@mui/material';
import { DataGrid, GridColDef } from '@mui/x-data-grid';
import axios from 'axios';
import React, { useEffect, useState } from 'react';
import { authenticate, getToken } from '../auth';

interface Device {
    device_name: string;
    model: string;
    loopback: string;
    status: string;
    vendor: string;
    router_type: string;
    pollercluster: string;
    poll_interval: string;
    network: string;
    last_update: string;
    phys_ip_address: string;
}

const DashboardGridData: React.FC = () => {
    const [selectedDeviceName, setSelectedDeviceName] = useState<string>('');
    const [selectedDeviceIp, setSelectedDeviceIp] = useState<string>('');
    const [selectedDeviceModel, setSelectedDeviceModel] = useState<string>('');
    const [selectedDeviceVendor, setSelectedDeviceVendor] = useState<string>('');
    const [selectedDeviceNetwork, setSelectedDeviceNetwork] = useState<string>('');
    const [data, setData] = useState<Device[]>([]);
    const [loading, setLoading] = useState<boolean>(false);

    const [deviceModels, setDeviceModels] = useState<string[]>([]);
    const [deviceVendors, setDeviceVendors] = useState<string[]>([]);
    const [deviceNetworks, setDeviceNetworks] = useState<string[]>([]);
    const [dropdownLoading, setDropdownLoading] = useState<boolean>(true);

    useEffect(() => {
        const fetchDropDownData = async () => {
            try {
                setDropdownLoading(true);
                await authenticate("Eclipse", __UCS_API_PW__);
                const token = getToken();
                const headers = { Authorization: `Bearer ${token}` };
                const [modelsResponse, vendorsResponse, networksResponse] = await Promise.all([
                    axios.get<string[]>("/portal/unique/models", { headers }),
                    axios.get<string[]>("/portal/unique/vendors", { headers }),
                    axios.get<string[]>("/portal/unique/networks", { headers }),
                ]);
                setDeviceModels(modelsResponse.data.length ? modelsResponse.data : ["No options available"]);
                setDeviceVendors(vendorsResponse.data.length ? vendorsResponse.data : ["No options available"]);
                setDeviceNetworks(networksResponse.data.length ? networksResponse.data : ["No options available"]);
            } catch (error) {
                console.error('Error fetching dropdown data', error);
                setDeviceModels(["Failed to load options"]);
                setDeviceVendors(["Failed to load options"]);
                setDeviceNetworks(["Failed to load options"]);
            } finally {
                setDropdownLoading(false);
            }
        };

        fetchDropDownData();
    }, []);

    const handleSelectChange = (setter: React.Dispatch<React.SetStateAction<string>>, resetOthers: () => void) => (event: SelectChangeEvent<string>) => {
        resetOthers();
        setter(event.target.value as string);
    };

    const handleTextFieldChange = (setter: React.Dispatch<React.SetStateAction<string>>, resetOthers: () => void) => (event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
        resetOthers();
        setter(event.target.value);
    };

    const resetSelections = () => {
        setSelectedDeviceName('');
        setSelectedDeviceIp('');
        setSelectedDeviceModel('');
        setSelectedDeviceVendor('');
        setSelectedDeviceNetwork('');
    };

    const handleSubmit = async () => {
        setLoading(true);
        try {
            const token = getToken();
            const headers = { Authorization: `Bearer ${token}` };
            let response;
            if (selectedDeviceName) {
                response = await axios.get<Device[]>("/portal/get-devices", {
                    params: { deviceName: selectedDeviceName },
                    headers
                });
            } else if (selectedDeviceIp) {
                response = await axios.get<Device[]>("/portal/get-devices", {
                    params: { loopback: selectedDeviceIp },
                    headers
                });
            } else if (selectedDeviceModel) {
                response = await axios.get<Device[]>("/portal/get-devices", {
                    params: { model: selectedDeviceModel },
                    headers
                });
            } else if (selectedDeviceVendor) {
                response = await axios.get<Device[]>("/portal/get-devices", {
                    params: { vendor: selectedDeviceVendor },
                    headers
                });
            } else if (selectedDeviceNetwork) {
                response = await axios.get<Device[]>("/portal/get-devices", {
                    params: { network: selectedDeviceNetwork },
                    headers
                });
            }
            if (response) {
                setData(response.data);
            }
        } catch (error) {
            console.error('Error fetching data', error);
        } finally {
            setLoading(false);
        }
    };

    const columns: GridColDef[] = [
        { field: 'device_name', headerName: 'Device Name', minWidth: 150, flex: 1 },
        { field: 'model', headerName: 'Model', minWidth: 150, flex: 1 },
        { field: 'loopback', headerName: 'Loopback', minWidth: 150, flex: 1 },
        { field: 'status', headerName: 'Status', minWidth: 150, flex: 1 },
        { field: 'vendor', headerName: 'Vendor', minWidth: 150, flex: 1 },
        { field: 'router_type', headerName: 'Router Type', minWidth: 150, flex: 1 },
        { field: 'pollercluster', headerName: 'Poller Cluster', minWidth: 150, flex: 1 },
        { field: 'poll_interval', headerName: 'Poll Interval', minWidth: 150, flex: 1 },
        { field: 'network', headerName: 'Network', minWidth: 150, flex: 1 },
        { field: 'last_update', headerName: 'Last Update', minWidth: 150, flex: 1 },
        { field: 'phys_ip_address', headerName: 'Physical IP', minWidth: 150, flex: 1 },
    ];

    return (
        <Container maxWidth="lg">
            <Typography gutterBottom variant="h4" className="header" fontWeight={"bold"} paddingTop={"15px"}>
                UCS Portal
            </Typography>
            <Card sx={{ boxShadow: 1, border: 1 }}>
                <CardContent>
                    <Grid container spacing={2}>
                        <Grid item xs={12} sm={6} md={3}>
                            <TextField
                                placeholder="Enter Device Name"
                                fullWidth
                                variant="outlined"
                                value={selectedDeviceName}
                                onChange={(event) => handleTextFieldChange(setSelectedDeviceName, resetSelections)(event)}
                                label="Device Name"
                            />
                        </Grid>
                        <Grid item xs={12} sm={6} md={3}>
                            <TextField
                                placeholder="Enter Device IP"
                                fullWidth
                                variant="outlined"
                                value={selectedDeviceIp}
                                onChange={(event) => handleTextFieldChange(setSelectedDeviceIp, resetSelections)(event)}
                                label="Device IP"
                            />
                        </Grid>
                        <Grid item xs={12} sm={6} md={3}>
                            <FormControl fullWidth variant="outlined">
                                <InputLabel id="device-model-select-label">Device Model</InputLabel>
                                <Select
                                    labelId="device-model-select-label"
                                    value={selectedDeviceModel}
                                    onChange={(event) => handleSelectChange(setSelectedDeviceModel, resetSelections)(event)}
                                    label="Device Model"
                                >
                                    {deviceModels.map((model) => (
                                        <MenuItem key={model} value={model}>
                                            {model}
                                        </MenuItem>
                                    ))}
                                </Select>
                            </FormControl>
                        </Grid>
                        <Grid item xs={12} sm={6} md={3}>
                            <FormControl fullWidth variant="outlined">
                                <InputLabel id="device-vendor-select-label">Device Vendor</InputLabel>
                                <Select
                                    labelId="device-vendor-select-label"
                                    value={selectedDeviceVendor}
                                    onChange={(event) => handleSelectChange(setSelectedDeviceVendor, resetSelections)(event)}
                                    label="Device Vendor"
                                >
                                    {deviceVendors.map((vendor) => (
                                        <MenuItem key={vendor} value={vendor}>
                                            {vendor}
                                        </MenuItem>
                                    ))}
                                </Select>
                            </FormControl>
                        </Grid>
                        <Grid item xs={12} sm={6} md={3}>
                            <FormControl fullWidth variant="outlined">
                                <InputLabel id="device-network-select-label">Device Network</InputLabel>
                                <Select
                                    labelId="device-network-select-label"
                                    value={selectedDeviceNetwork}
                                    onChange={(event) => handleSelectChange(setSelectedDeviceNetwork, resetSelections)(event)}
                                    label="Device Network"
                                >
                                    {deviceNetworks.map((network) => (
                                        <MenuItem key={network} value={network}>
                                            {network}









package com.verizon.ucs.restapi.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.verizon.ucs.restapi.model.Device;

public interface UCSPRepository extends JpaRepository<Device, Long> {

    List<Device> findByDeviceNameIgnoreCase(String deviceName);
    List<Device> findByLoopbackIgnoreCase(String loopback);
    List<Device> findByNetworkIgnoreCase(String network);
    List<Device> findByVendorIgnoreCase(String vendor);
    List<Device> findByModelIgnoreCase(String model);

    @Query("SELECT DISTINCT d.model FROM Device d")
    List<String> findDistinctModels();

    @Query("SELECT DISTINCT d.vendor FROM Device d")
    List<String> findDistinctVendors();

    @Query("SELECT DISTINCT d.network FROM Device d")
    List<String> findDistinctNetworks();
}






package com.verizon.ucs.restapi.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.verizon.ucs.restapi.model.Device;
import com.verizon.ucs.restapi.repository.UCSPRepository;

@Service
public class UCSPService {

    @Autowired
    private UCSPRepository uCSPRepository;

    public List<Device> searchDevices(ApiRequest params) {
        if (params.getDeviceName() != null && !params.getDeviceName().isEmpty()) {
            return uCSPRepository.findByDeviceNameIgnoreCase(params.getDeviceName());
        }
        if (params.getLoopback() != null && !params.getLoopback().isEmpty()) {
            return uCSPRepository.findByLoopbackIgnoreCase(params.getLoopback());
        }
        if (params.getNetwork() != null && !params.getNetwork().isEmpty()) {
            return uCSPRepository.findByNetworkIgnoreCase(params.getNetwork());
        }
        if (params.getVendor() != null && !params.getVendor().isEmpty()) {
            return uCSPRepository.findByVendorIgnoreCase(params.getVendor());
        }
        if (params.getModel() != null && !params.getModel().isEmpty()) {
            return uCSPRepository.findByModelIgnoreCase(params.getModel());
        }
        return null;
    }

    public Map<String, List<String>> getUniqueValues() {
        Map<String, List<String>> uniqueValues = new HashMap<>();
        uniqueValues.put("models", uCSPRepository.findDistinctModels());
        uniqueValues.put("vendors", uCSPRepository.findDistinctVendors());
        uniqueValues.put("networks", uCSPRepository.findDistinctNetworks());
        return uniqueValues;
    }
}








package com.verizon.ucs.restapi.controllers;

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

	@Operation(summary = "Fetch unique models for dropdown population")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "OK.") })
	@GetMapping("/unique/models")
	public ResponseEntity<?> getUniqueModels() {
		try {
			List<String> models = uCSPService.getUniqueValues().get("models");
			return ResponseEntity.ok(models);
		} catch (Exception e) {
			logger.error("Error fetching unique models: ", e);
			return new ResponseEntity<>("Failed to fetch models", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Operation(summary = "Fetch unique vendors for dropdown population")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "OK.") })
	@GetMapping("/unique/vendors")
	public ResponseEntity<?> getUniqueVendors() {
		try {
			List<String> vendors = uCSPService.getUniqueValues().get("vendors");
			return ResponseEntity.ok(vendors);
		} catch (Exception e) {
			logger.error("Error fetching unique vendors: ", e);
			return new ResponseEntity<>("Failed to fetch vendors", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Operation(summary = "Fetch unique networks for dropdown population")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "OK.") })
	@GetMapping("/unique/networks")
	public ResponseEntity<?> getUniqueNetworks() {
		try {
			List<String> networks = uCSPService.getUniqueValues().get("networks");
			return ResponseEntity.ok(networks);
		} catch (Exception e) {
			logger.error("Error fetching unique networks: ", e);
			return new ResponseEntity<>("Failed to fetch networks", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private String createErrorMessage(String endpoint, Exception e) {
		return "Error: " + e.getMessage() + " In Class: " + e.getStackTrace()[0].getClassName()
				+ " In Method: " + e.getStackTrace()[0].getMethodName() + " at line number: "
				+ e.getStackTrace()[0].getLineNumber() + " at endpoint: " + endpoint;
	}
}
