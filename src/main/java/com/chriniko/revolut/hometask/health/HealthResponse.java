package com.chriniko.revolut.hometask.health;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HealthResponse {

    private HealthStatus status;
    private String message;


    public static enum HealthStatus {
        OK, DOWN
    }
}
