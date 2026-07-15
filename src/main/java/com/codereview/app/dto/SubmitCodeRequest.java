package com.codereview.app.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SubmitCodeRequest {

    @NotBlank
    private String code;

    @NotBlank
    private String fileName;
}