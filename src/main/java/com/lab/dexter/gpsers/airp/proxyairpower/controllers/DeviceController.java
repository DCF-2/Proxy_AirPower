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
            @RequestHeader("X-ThingsBoard-URL") String tbUrl, // 1. Recebe a URL
            @RequestParam(defaultValue = "100") int pageSize,
            @RequestParam(defaultValue = "0") int page) {

        JsonNode devices = deviceProxyService.getTenantDevices(token, tbUrl, pageSize, page); // Passa a URL
        return ResponseEntity.ok(devices);
    }

    // POST /api/devices
    @PostMapping
    public ResponseEntity<JsonNode> createDevice(
            @RequestHeader("Authorization") String token,
            @RequestHeader("X-ThingsBoard-URL") String tbUrl, // 2. Recebe a URL
            @RequestBody CreateDeviceRequest request) {

        JsonNode newDevice = deviceProxyService.createDevice(token, tbUrl, request); // Passa a URL
        return ResponseEntity.ok(newDevice);
    }

    // GET /api/devices/{id}/credentials
    @GetMapping("/{id}/credentials")
    public ResponseEntity<JsonNode> getCredentials(
            @RequestHeader("Authorization") String token,
            @RequestHeader("X-ThingsBoard-URL") String tbUrl, // 3. Recebe a URL
            @PathVariable("id") String deviceId) {

        JsonNode credentials = deviceProxyService.getDeviceCredentials(token, tbUrl, deviceId); // Passa a URL
        return ResponseEntity.ok(credentials);
    }

    // POST /api/devices/{id}/location
    @PostMapping("/{id}/location")
    public ResponseEntity<Void> saveLocation(
            @RequestHeader("Authorization") String token,
            @RequestHeader("X-ThingsBoard-URL") String tbUrl, // 4. Recebe a URL
            @PathVariable("id") String deviceId,
            @RequestBody LocationRequest location) {

        deviceProxyService.saveDeviceLocation(token, tbUrl, deviceId, location); // Passa a URL
        return ResponseEntity.ok().build();
    }
}