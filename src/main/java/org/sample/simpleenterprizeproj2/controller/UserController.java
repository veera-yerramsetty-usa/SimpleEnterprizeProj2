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
import org.sample.simpleenterprizeproj2.dto.UserPatchRequest;
import org.sample.simpleenterprizeproj2.dto.UserRequest;
import org.sample.simpleenterprizeproj2.dto.UserResponse;
import org.sample.simpleenterprizeproj2.service.UserService;
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
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User management operations")
public class UserController {

    private final UserService userService;
    private final PagedResourcesAssembler<UserResponse> pagedAssembler;

    public UserController(UserService userService,
                          PagedResourcesAssembler<UserResponse> pagedAssembler) {
        this.userService = userService;
        this.pagedAssembler = pagedAssembler;
    }

    @GetMapping
    @Operation(summary = "List users", description = "Retrieve a paginated list of users with optional filters")
    @ApiResponse(responseCode = "200", description = "Users retrieved successfully")
    @ApiResponse(responseCode = "400", description = "Invalid query parameter",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    public ResponseEntity<PagedModel<EntityModel<UserResponse>>> getAll(
            @Parameter(description = "Filter by username (partial match)") @RequestParam(required = false) @Size(max = 255) String username,
            @Parameter(description = "Filter by email (partial match)") @RequestParam(required = false) @Size(max = 255) String email,
            @Parameter(description = "Filter by role (partial match)") @RequestParam(required = false) @Size(max = 255) String role,
            Pageable pageable) {
        Page<UserResponse> page = userService.findAll(username, email, role, pageable);
        return ResponseEntity.ok(pagedAssembler.toModel(page,
                response -> toEntityModel(response)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Retrieve a single user by their ID")
    @ApiResponse(responseCode = "200", description = "User found")
    @ApiResponse(responseCode = "404", description = "User not found",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    public ResponseEntity<EntityModel<UserResponse>> getById(
            @Parameter(description = "User ID") @PathVariable Long id) {
        return ResponseEntity.ok(toEntityModel(userService.findResponseById(id)));
    }

    @PostMapping
    @Operation(summary = "Create user", description = "Create a new user")
    @ApiResponse(responseCode = "201", description = "User created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request body",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "409", description = "User with given unique field(s) already exists",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    public ResponseEntity<EntityModel<UserResponse>> create(@Valid @RequestBody UserRequest request) {
        UserResponse response = userService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(response.getId()).toUri();
        return ResponseEntity.created(location).body(toEntityModel(response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user", description = "Fully replace an existing user")
    @ApiResponse(responseCode = "200", description = "User updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request body",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "404", description = "User not found",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "409", description = "User with given unique field(s) already exists",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    public ResponseEntity<EntityModel<UserResponse>> update(
            @Parameter(description = "User ID") @PathVariable Long id,
            @Valid @RequestBody UserRequest request) {
        return ResponseEntity.ok(toEntityModel(userService.update(id, request)));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Partially update user", description = "Update specific fields of an existing user")
    @ApiResponse(responseCode = "200", description = "User patched successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request body",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "404", description = "User not found",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "409", description = "User with given unique field(s) already exists",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    public ResponseEntity<EntityModel<UserResponse>> patch(
            @Parameter(description = "User ID") @PathVariable Long id,
            @Valid @RequestBody UserPatchRequest request) {
        return ResponseEntity.ok(toEntityModel(userService.patch(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user", description = "Delete a user by their ID")
    @ApiResponse(responseCode = "204", description = "User deleted successfully")
    @ApiResponse(responseCode = "404", description = "User not found",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    public ResponseEntity<Void> delete(
            @Parameter(description = "User ID") @PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bulk")
    @Operation(summary = "Bulk create users", description = "Create multiple users in a single request")
    @ApiResponse(responseCode = "201", description = "Users created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request body",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "409", description = "User with given unique field(s) already exists",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    public ResponseEntity<List<EntityModel<UserResponse>>> bulkCreate(
            @RequestBody @NotEmpty @Size(max = 100) List<@Valid UserRequest> requests) {
        List<UserResponse> responses = userService.bulkCreate(requests);
        List<EntityModel<UserResponse>> models = responses.stream()
                .map(this::toEntityModel)
                .toList();
        return ResponseEntity.status(201).body(models);
    }

    @PutMapping("/bulk")
    @Operation(summary = "Bulk update users", description = "Update multiple users in a single request")
    @ApiResponse(responseCode = "200", description = "Users updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request body",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "404", description = "User not found",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "409", description = "User with given unique field(s) already exists",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    public ResponseEntity<List<EntityModel<UserResponse>>> bulkUpdate(
            @RequestBody @NotEmpty @Size(max = 100) List<@Valid BulkUpdateRequest<UserRequest>> requests) {
        List<UserResponse> responses = userService.bulkUpdate(requests);
        List<EntityModel<UserResponse>> models = responses.stream()
                .map(this::toEntityModel)
                .toList();
        return ResponseEntity.ok(models);
    }

    @DeleteMapping("/bulk")
    @Operation(summary = "Bulk delete users", description = "Delete multiple users in a single request")
    @ApiResponse(responseCode = "204", description = "Users deleted successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request body",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "404", description = "User not found",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    public ResponseEntity<Void> bulkDelete(
            @RequestBody @NotEmpty @Size(max = 100) List<Long> ids) {
        userService.bulkDelete(ids);
        return ResponseEntity.noContent().build();
    }

    private EntityModel<UserResponse> toEntityModel(UserResponse response) {
        return EntityModel.of(response,
                linkTo(methodOn(UserController.class).getById(response.getId())).withSelfRel(),
                linkTo(methodOn(UserController.class).getAll(null, null, null, null)).withRel("users"));
    }
}
