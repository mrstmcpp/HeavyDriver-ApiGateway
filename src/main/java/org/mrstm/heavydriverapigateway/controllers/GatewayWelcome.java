package org.mrstm.heavydriverapigateway.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GatewayWelcome {
    @GetMapping("/")
    public ResponseEntity<String> gatewayWelcome(){
        return ResponseEntity.ok("Welcome bhai welcome to HeavyDriver Api Gatewayyyyyyyyyyyyy.....");
    }
}
