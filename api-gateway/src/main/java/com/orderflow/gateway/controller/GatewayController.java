package com.orderflow.gateway.controller;

import com.orderflow.gateway.proxy.GatewayProxyService;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class GatewayController {

    private final GatewayProxyService
            proxyService;

    public GatewayController(
            GatewayProxyService proxyService
    ) {
        this.proxyService =
                proxyService;
    }

    @RequestMapping("/api/v1/**")
    public ResponseEntity<byte[]> proxy(
            HttpServletRequest request
    ) throws IOException {

        return proxyService.forward(
                request
        );
    }
}