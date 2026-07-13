package com.tufin.policyengine.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyRule {

    private String id;
    private String sourceIp;
    private String destinationIp;
    private int port;
    private Action action;
    private int priority;
}
