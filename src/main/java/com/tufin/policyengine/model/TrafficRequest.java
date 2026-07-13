package com.tufin.policyengine.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrafficRequest {

    private String sourceIp;
    private String destinationIp;
    private int port;
}
