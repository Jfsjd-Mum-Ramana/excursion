package com.verizon.ucs.restapi.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.verizon.ucs.restapi.service.UCSPService;

@RestController
@RequestMapping("/api/unique")
public class UCSPController {

    @Autowired
    private UCSPService uCSPService;

    @GetMapping("/models")
    public ResponseEntity<List<String>> getUniqueModels() {
        return ResponseEntity.ok(uCSPService.getUniqueValues().get("models"));
    }

    @GetMapping("/vendors")
    public ResponseEntity<List<String>> getUniqueVendors() {
        return ResponseEntity.ok(uCSPService.getUniqueValues().get("vendors"));
    }

    @GetMapping("/networks")
    public ResponseEntity<List<String>> getUniqueNetworks() {
        return ResponseEntity.ok(uCSPService.getUniqueValues().get("networks"));
    }

    // Existing endpoints...
}





package com.verizon.ucs.restapi.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.verizon.ucs.restapi.repository.UCSPRepository;

@Service
public class UCSPService {

    @Autowired
    private UCSPRepository uCSPRepository;

    // Method to get unique values for dropdowns
    public Map<String, List<String>> getUniqueValues() {
        Map<String, List<String>> uniqueValues = new HashMap<>();
        uniqueValues.put("models", uCSPRepository.findDistinctModels());
        uniqueValues.put("vendors", uCSPRepository.findDistinctVendors());
        uniqueValues.put("networks", uCSPRepository.findDistinctNetworks());
        return uniqueValues;
    }

    // Existing methods...
}





package com.verizon.ucs.restapi.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.verizon.ucs.restapi.model.Device;

@Repository
public interface UCSPRepository extends JpaRepository<Device, Long> {

    // Add methods to fetch distinct values
    @Query("SELECT DISTINCT d.model FROM Device d WHERE d.model IS NOT NULL")
    List<String> findDistinctModels();

    @Query("SELECT DISTINCT d.vendor FROM Device d WHERE d.vendor IS NOT NULL")
    List<String> findDistinctVendors();

    @Query("SELECT DISTINCT d.network FROM Device d WHERE d.network IS NOT NULL")
    List<String> findDistinctNetworks();

    // Existing search methods...
}

