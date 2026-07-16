package com.tufin.policyengine.controller;

import com.tufin.policyengine.exception.DuplicateRuleNameException;
import com.tufin.policyengine.exception.GlobalExceptionHandler;
import com.tufin.policyengine.exception.RuleNotFoundException;
import com.tufin.policyengine.service.RuleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private RuleService ruleService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new RuleController(ruleService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returns404WhenRuleNotFound() throws Exception {
        when(ruleService.getRuleById("missing-id"))
                .thenThrow(new RuleNotFoundException("missing-id"));

        mockMvc.perform(get("/api/v1/rules/missing-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", is("Rule not found: missing-id")))
                .andExpect(jsonPath("$.path", is("/api/v1/rules/missing-id")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    void returns400ForMissingRequiredField() throws Exception {
        String bodyMissingName = """
                {
                  "priority": 10,
                  "resource": "/api/*",
                  "action": "READ",
                  "subject": "ADMIN",
                  "decision": "ALLOW"
                }
                """;

        mockMvc.perform(post("/api/v1/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyMissingName))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", notNullValue()))
                .andExpect(jsonPath("$.path", is("/api/v1/rules")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    void returns400ForInvalidDecisionValue() throws Exception {
        String bodyBadDecision = """
                {
                  "name": "Test Rule",
                  "priority": 10,
                  "resource": "/api/*",
                  "action": "READ",
                  "subject": "ADMIN",
                  "decision": "MAYBE"
                }
                """;

        mockMvc.perform(post("/api/v1/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyBadDecision))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", notNullValue()))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    void returns400ForMalformedJson() throws Exception {
        mockMvc.perform(post("/api/v1/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ this is not valid json }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", is("Malformed or missing request body")))
                .andExpect(jsonPath("$.path", is("/api/v1/rules")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    void returns409ForDuplicateRuleName() throws Exception {
        when(ruleService.createRule(any()))
                .thenThrow(new DuplicateRuleNameException("Allow Admins"));

        String validBody = """
                {
                  "name": "Allow Admins",
                  "priority": 10,
                  "resource": "/api/*",
                  "action": "READ",
                  "subject": "ADMIN",
                  "decision": "ALLOW"
                }
                """;

        mockMvc.perform(post("/api/v1/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.message", is("A rule with the same name already exists: Allow Admins")))
                .andExpect(jsonPath("$.path", is("/api/v1/rules")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    void returns500ForUnexpectedException() throws Exception {
        when(ruleService.getAllRules())
                .thenThrow(new RuntimeException("Unexpected storage failure"));

        mockMvc.perform(get("/api/v1/rules"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status", is(500)))
                .andExpect(jsonPath("$.error", is("Internal Server Error")))
                .andExpect(jsonPath("$.message", is("An unexpected error occurred")))
                .andExpect(jsonPath("$.path", is("/api/v1/rules")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    void returns405ForUnsupportedHttpMethod() throws Exception {
        mockMvc.perform(patch("/api/v1/rules"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status", is(405)))
                .andExpect(jsonPath("$.error", is("Method Not Allowed")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    void returns404WhenDeletingNonExistentRule() throws Exception {
        doThrow(new RuleNotFoundException("ghost-id"))
                .when(ruleService).deleteRule(anyString());

        mockMvc.perform(delete("/api/v1/rules/ghost-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("Rule not found: ghost-id")));
    }
}
