package org.sample.simpleenterprizeproj2.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class DepartmentRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    @Pattern(regexp = "^[a-zA-Z '-]+$", message = "Name must contain only letters, spaces, hyphens, or apostrophes")
    private String name;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    @Pattern(regexp = "^[a-zA-Z0-9 .,;:!?'\"()\\-]*$", message = "Description must contain only letters, digits, spaces, or common punctuation")
    private String description;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
