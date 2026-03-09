package com.lab.dexter.gpsers.airp.proxyairpower.controllers;

import com.lab.dexter.gpsers.airp.proxyairpower.dtos.CreateDeviceRequest;
import com.lab.dexter.gpsers.airp.proxyairpower.dtos.LocationRequest;
import com.lab.dexter.gpsers.airp.proxyairpower.services.DeviceProxyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceProxyService deviceProxyService;

    public DeviceController(DeviceProxyService deviceProxyService) {
        this.deviceProxyService = deviceProxyService;
    }

    // GET /api/devices
    @GetMapping
    public ResponseEntity<JsonNode> getDevices(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "100") int pageSize,
            @RequestParam(defaultValue = "0") int page) {

        JsonNode devices = deviceProxyService.getTenantDevices(token, pageSize, page);
        return ResponseEntity.ok(devices);
    }

    // POST /api/devices
    @PostMapping
    public ResponseEntity<JsonNode> createDevice(
            @RequestHeader("Authorization") String token,
            @RequestBody CreateDeviceRequest request) {

        JsonNode newDevice = deviceProxyService.createDevice(token, request);
        return ResponseEntity.ok(newDevice);
    }

    // GET /api/devices/{id}/credentials
    @GetMapping("/{id}/credentials")
    public ResponseEntity<JsonNode> getCredentials(
            @RequestHeader("Authorization") String token,
            @PathVariable("id") String deviceId) {

        JsonNode credentials = deviceProxyService.getDeviceCredentials(token, deviceId);
        return ResponseEntity.ok(credentials);
    }

    // POST /api/devices/{id}/location
    @PostMapping("/{id}/location")
    public ResponseEntity<Void> saveLocation(
            @RequestHeader("Authorization") String token,
            @PathVariable("id") String deviceId,
            @RequestBody LocationRequest location) {

        deviceProxyService.saveDeviceLocation(token, deviceId, location);
        return ResponseEntity.ok().build();
    }
}