package com.codereview.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @NotBlank
    @Pattern(
            regexp = "^[A-Za-z][A-Za-z '-]{1,49}$",
            message = "Name must start with a letter and contain only letters, spaces, hyphens, or apostrophes"
    )
    private String name;
}