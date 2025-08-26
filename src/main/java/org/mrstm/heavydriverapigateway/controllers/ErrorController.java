package org.mrstm.heavydriverapigateway.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController

public class ErrorController {
    @GetMapping("/auths")
    public ResponseEntity<String> showError(){
        return ResponseEntity.ok("404 h bhai");
    }
}
