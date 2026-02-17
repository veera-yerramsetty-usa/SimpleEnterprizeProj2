package org.sample.simpleenterprizeproj2.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class DepartmentRequest {

    @NotBlank(message = "{validation.department.name.required}")
    @Size(max = 255, message = "{validation.department.name.size}")
    @Pattern(regexp = "^[a-zA-Z '-]+$", message = "{validation.department.name.pattern}")
    private String name;

    @Size(max = 255, message = "{validation.department.description.size}")
    @Pattern(regexp = "^[a-zA-Z0-9 .,;:!?'\"()\\-]*$", message = "{validation.department.description.pattern}")
    private String description;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
