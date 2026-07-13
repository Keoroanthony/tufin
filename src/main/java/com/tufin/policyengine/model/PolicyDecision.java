package com.tufin.policyengine.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyDecision {

    private Action decision;
    private String matchedRuleId;
    private String reason;
}
