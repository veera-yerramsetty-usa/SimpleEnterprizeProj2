package org.sample.simpleenterprizeproj2.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.sample.simpleenterprizeproj2.dto.BulkUpdateRequest;
import org.sample.simpleenterprizeproj2.dto.DepartmentPatchRequest;
import org.sample.simpleenterprizeproj2.dto.DepartmentRequest;
import org.sample.simpleenterprizeproj2.dto.DepartmentResponse;
import org.sample.simpleenterprizeproj2.service.DepartmentService;
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
import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Validated
@RestController
@RequestMapping("/api/v1/departments")
@Tag(name = "Departments", description = "Department management operations")
public class DepartmentController {

    private final DepartmentService departmentService;
    private final PagedResourcesAssembler<DepartmentResponse> pagedAssembler;

    public DepartmentController(DepartmentService departmentService,
                                PagedResourcesAssembler<DepartmentResponse> pagedAssembler) {
        this.departmentService = departmentService;
        this.pagedAssembler = pagedAssembler;
    }

    @GetMapping
    @Operation(summary = "List departments", description = "Retrieve a paginated list of departments with optional filters")
    @ApiResponse(responseCode = "200", description = "Departments retrieved successfully")
    @ApiResponse(responseCode = "400", description = "Invalid query parameter",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    public ResponseEntity<PagedModel<EntityModel<DepartmentResponse>>> getAll(
            @Parameter(description = "Filter by department name (partial match)") @RequestParam(required = false) @Size(max = 255) String name,
            Pageable pageable) {
        Page<DepartmentResponse> page = departmentService.findAll(name, pageable);
        return ResponseEntity.ok(pagedAssembler.toModel(page,
                response -> toEntityModel(response)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get department by ID", description = "Retrieve a single department by its ID")
    @ApiResponse(responseCode = "200", description = "Department found")
    @ApiResponse(responseCode = "404", description = "Department not found",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    public ResponseEntity<EntityModel<DepartmentResponse>> getById(
            @Parameter(description = "Department ID") @PathVariable Long id) {
        return ResponseEntity.ok(toEntityModel(departmentService.findResponseById(id)));
    }

    @PostMapping
    @Operation(summary = "Create department", description = "Create a new department")
    @ApiResponse(responseCode = "201", description = "Department created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request body",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "409", description = "Department with given unique field(s) already exists",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    public ResponseEntity<EntityModel<DepartmentResponse>> create(
            @Valid @RequestBody DepartmentRequest request) {
        DepartmentResponse response = departmentService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(response.getId()).toUri();
        return ResponseEntity.created(location).body(toEntityModel(response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update department", description = "Fully replace an existing department")
    @ApiResponse(responseCode = "200", description = "Department updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request body",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "404", description = "Department not found",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "409", description = "Department with given unique field(s) already exists",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    public ResponseEntity<EntityModel<DepartmentResponse>> update(
            @Parameter(description = "Department ID") @PathVariable Long id,
            @Valid @RequestBody DepartmentRequest request) {
        return ResponseEntity.ok(toEntityModel(departmentService.update(id, request)));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Partially update department", description = "Update specific fields of an existing department")
    @ApiResponse(responseCode = "200", description = "Department patched successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request body",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "404", description = "Department not found",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "409", description = "Department with given unique field(s) already exists",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    public ResponseEntity<EntityModel<DepartmentResponse>> patch(
            @Parameter(description = "Department ID") @PathVariable Long id,
            @Valid @RequestBody DepartmentPatchRequest request) {
        return ResponseEntity.ok(toEntityModel(departmentService.patch(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete department", description = "Delete a department by its ID")
    @ApiResponse(responseCode = "204", description = "Department deleted successfully")
    @ApiResponse(responseCode = "404", description = "Department not found",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    public ResponseEntity<Void> delete(
            @Parameter(description = "Department ID") @PathVariable Long id) {
        departmentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bulk")
    @Operation(summary = "Bulk create departments", description = "Create multiple departments in a single request")
    @ApiResponse(responseCode = "201", description = "Departments created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request body",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "409", description = "Department with given unique field(s) already exists",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    public ResponseEntity<List<EntityModel<DepartmentResponse>>> bulkCreate(
            @RequestBody @NotEmpty @Size(max = 100) List<@Valid DepartmentRequest> requests) {
        List<DepartmentResponse> responses = departmentService.bulkCreate(requests);
        List<EntityModel<DepartmentResponse>> models = responses.stream()
                .map(this::toEntityModel)
                .toList();
        return ResponseEntity.status(201).body(models);
    }

    @PutMapping("/bulk")
    @Operation(summary = "Bulk update departments", description = "Update multiple departments in a single request")
    @ApiResponse(responseCode = "200", description = "Departments updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request body",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "404", description = "Department not found",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "409", description = "Department with given unique field(s) already exists",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    public ResponseEntity<List<EntityModel<DepartmentResponse>>> bulkUpdate(
            @RequestBody @NotEmpty @Size(max = 100) List<@Valid BulkUpdateRequest<DepartmentRequest>> requests) {
        List<DepartmentResponse> responses = departmentService.bulkUpdate(requests);
        List<EntityModel<DepartmentResponse>> models = responses.stream()
                .map(this::toEntityModel)
                .toList();
        return ResponseEntity.ok(models);
    }

    @DeleteMapping("/bulk")
    @Operation(summary = "Bulk delete departments", description = "Delete multiple departments in a single request")
    @ApiResponse(responseCode = "204", description = "Departments deleted successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request body",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "404", description = "Department not found",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    public ResponseEntity<Void> bulkDelete(
            @RequestBody @NotEmpty @Size(max = 100) List<Long> ids) {
        departmentService.bulkDelete(ids);
        return ResponseEntity.noContent().build();
    }

    private EntityModel<DepartmentResponse> toEntityModel(DepartmentResponse response) {
        return EntityModel.of(response,
                linkTo(methodOn(DepartmentController.class).getById(response.getId())).withSelfRel(),
                linkTo(methodOn(DepartmentController.class).getAll(null, null)).withRel("departments"));
    }
}
