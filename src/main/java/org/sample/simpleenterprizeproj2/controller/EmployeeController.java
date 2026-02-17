package org.sample.simpleenterprizeproj2.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.sample.simpleenterprizeproj2.dto.EmployeePatchRequest;
import org.sample.simpleenterprizeproj2.dto.EmployeeRequest;
import org.sample.simpleenterprizeproj2.dto.EmployeeResponse;
import org.sample.simpleenterprizeproj2.service.EmployeeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Validated
@RestController
@RequestMapping("/api/v1/employees")
@Tag(name = "Employees", description = "Employee management operations")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final PagedResourcesAssembler<EmployeeResponse> pagedAssembler;

    public EmployeeController(EmployeeService employeeService,
                              PagedResourcesAssembler<EmployeeResponse> pagedAssembler) {
        this.employeeService = employeeService;
        this.pagedAssembler = pagedAssembler;
    }

    @GetMapping
    @Operation(summary = "List employees", description = "Retrieve a paginated list of employees with optional filters")
    @ApiResponse(responseCode = "200", description = "Employees retrieved successfully")
    @ApiResponse(responseCode = "400", description = "Invalid query parameter",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    public ResponseEntity<PagedModel<EntityModel<EmployeeResponse>>> getAll(
            @Parameter(description = "Filter by first name (partial match)") @RequestParam(required = false) @Size(max = 255) String firstName,
            @Parameter(description = "Filter by last name (partial match)") @RequestParam(required = false) @Size(max = 255) String lastName,
            @Parameter(description = "Filter by email (partial match)") @RequestParam(required = false) @Size(max = 255) String email,
            @Parameter(description = "Filter by department ID") @RequestParam(required = false) Long departmentId,
            Pageable pageable) {
        Page<EmployeeResponse> page = employeeService.findAll(firstName, lastName, email, departmentId, pageable);
        return ResponseEntity.ok(pagedAssembler.toModel(page,
                response -> toEntityModel(response)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get employee by ID", description = "Retrieve a single employee by their ID")
    @ApiResponse(responseCode = "200", description = "Employee found")
    @ApiResponse(responseCode = "404", description = "Employee not found",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    public ResponseEntity<EntityModel<EmployeeResponse>> getById(
            @Parameter(description = "Employee ID") @PathVariable Long id) {
        return ResponseEntity.ok(toEntityModel(employeeService.findResponseById(id)));
    }

    @PostMapping
    @Operation(summary = "Create employee", description = "Create a new employee")
    @ApiResponse(responseCode = "201", description = "Employee created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request body",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "409", description = "Employee with given unique field(s) already exists",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    public ResponseEntity<EntityModel<EmployeeResponse>> create(
            @Valid @RequestBody EmployeeRequest request) {
        EmployeeResponse response = employeeService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(response.getId()).toUri();
        return ResponseEntity.created(location).body(toEntityModel(response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update employee", description = "Fully replace an existing employee")
    @ApiResponse(responseCode = "200", description = "Employee updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request body",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "404", description = "Employee not found",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "409", description = "Employee with given unique field(s) already exists",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    public ResponseEntity<EntityModel<EmployeeResponse>> update(
            @Parameter(description = "Employee ID") @PathVariable Long id,
            @Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.ok(toEntityModel(employeeService.update(id, request)));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Partially update employee", description = "Update specific fields of an existing employee")
    @ApiResponse(responseCode = "200", description = "Employee patched successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request body",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "404", description = "Employee not found",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "409", description = "Employee with given unique field(s) already exists",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    public ResponseEntity<EntityModel<EmployeeResponse>> patch(
            @Parameter(description = "Employee ID") @PathVariable Long id,
            @Valid @RequestBody EmployeePatchRequest request) {
        return ResponseEntity.ok(toEntityModel(employeeService.patch(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete employee", description = "Delete an employee by their ID")
    @ApiResponse(responseCode = "204", description = "Employee deleted successfully")
    @ApiResponse(responseCode = "404", description = "Employee not found",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    public ResponseEntity<Void> delete(
            @Parameter(description = "Employee ID") @PathVariable Long id) {
        employeeService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private EntityModel<EmployeeResponse> toEntityModel(EmployeeResponse response) {
        return EntityModel.of(response,
                linkTo(methodOn(EmployeeController.class).getById(response.getId())).withSelfRel(),
                linkTo(methodOn(EmployeeController.class).getAll(null, null, null, null, null)).withRel("employees"));
    }
}
