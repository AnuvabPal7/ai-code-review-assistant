package com.codereview.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class DocumentationResponse {
    private String classSummary;
    private List<MethodDoc> methods;

    @Data
    @AllArgsConstructor
    public static class MethodDoc {
        private String methodName;
        private String description;
        private List<String> parameters;
        private String returns;
    }
}