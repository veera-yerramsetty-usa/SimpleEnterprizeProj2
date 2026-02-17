package org.sample.simpleenterprizeproj2.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.sample.simpleenterprizeproj2.dto.WebhookDeliveryLogResponse;
import org.sample.simpleenterprizeproj2.dto.WebhookRegistrationPatchRequest;
import org.sample.simpleenterprizeproj2.dto.WebhookRegistrationRequest;
import org.sample.simpleenterprizeproj2.dto.WebhookRegistrationResponse;
import org.sample.simpleenterprizeproj2.service.WebhookService;
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
@RequestMapping("/api/v1/webhooks")
@Tag(name = "Webhooks", description = "Webhook registration and delivery management")
public class WebhookController {

    private final WebhookService webhookService;
    private final PagedResourcesAssembler<WebhookRegistrationResponse> pagedAssembler;
    private final PagedResourcesAssembler<WebhookDeliveryLogResponse> deliveryPagedAssembler;

    public WebhookController(WebhookService webhookService,
                             PagedResourcesAssembler<WebhookRegistrationResponse> pagedAssembler,
                             PagedResourcesAssembler<WebhookDeliveryLogResponse> deliveryPagedAssembler) {
        this.webhookService = webhookService;
        this.pagedAssembler = pagedAssembler;
        this.deliveryPagedAssembler = deliveryPagedAssembler;
    }

    @GetMapping
    @Operation(summary = "List webhooks", description = "Retrieve a paginated list of webhook registrations")
    @ApiResponse(responseCode = "200", description = "Webhooks retrieved successfully")
    @ApiResponse(responseCode = "400", description = "Invalid query parameter",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    public ResponseEntity<PagedModel<EntityModel<WebhookRegistrationResponse>>> getAll(Pageable pageable) {
        Page<WebhookRegistrationResponse> page = webhookService.findAll(pageable);
        return ResponseEntity.ok(pagedAssembler.toModel(page,
                response -> toEntityModel(response)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get webhook by ID", description = "Retrieve a single webhook registration by its ID")
    @ApiResponse(responseCode = "200", description = "Webhook found")
    @ApiResponse(responseCode = "404", description = "Webhook not found",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    public ResponseEntity<EntityModel<WebhookRegistrationResponse>> getById(
            @Parameter(description = "Webhook ID") @PathVariable Long id) {
        return ResponseEntity.ok(toEntityModel(webhookService.findById(id)));
    }

    @PostMapping
    @Operation(summary = "Create webhook", description = "Register a new webhook endpoint")
    @ApiResponse(responseCode = "201", description = "Webhook created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request body",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    public ResponseEntity<EntityModel<WebhookRegistrationResponse>> create(
            @Valid @RequestBody WebhookRegistrationRequest request) {
        WebhookRegistrationResponse response = webhookService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(response.getId()).toUri();
        return ResponseEntity.created(location).body(toEntityModel(response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update webhook", description = "Fully replace an existing webhook registration")
    @ApiResponse(responseCode = "200", description = "Webhook updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request body",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "404", description = "Webhook not found",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    public ResponseEntity<EntityModel<WebhookRegistrationResponse>> update(
            @Parameter(description = "Webhook ID") @PathVariable Long id,
            @Valid @RequestBody WebhookRegistrationRequest request) {
        return ResponseEntity.ok(toEntityModel(webhookService.update(id, request)));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Partially update webhook", description = "Update specific fields of an existing webhook registration")
    @ApiResponse(responseCode = "200", description = "Webhook patched successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request body",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "404", description = "Webhook not found",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    public ResponseEntity<EntityModel<WebhookRegistrationResponse>> patch(
            @Parameter(description = "Webhook ID") @PathVariable Long id,
            @Valid @RequestBody WebhookRegistrationPatchRequest request) {
        return ResponseEntity.ok(toEntityModel(webhookService.patch(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete webhook", description = "Delete a webhook registration and its delivery logs")
    @ApiResponse(responseCode = "204", description = "Webhook deleted successfully")
    @ApiResponse(responseCode = "404", description = "Webhook not found",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    public ResponseEntity<Void> delete(
            @Parameter(description = "Webhook ID") @PathVariable Long id) {
        webhookService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/deliveries")
    @Operation(summary = "List delivery logs", description = "Retrieve paginated delivery logs for a specific webhook")
    @ApiResponse(responseCode = "200", description = "Delivery logs retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Webhook not found",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    public ResponseEntity<PagedModel<EntityModel<WebhookDeliveryLogResponse>>> getDeliveryLogs(
            @Parameter(description = "Webhook ID") @PathVariable Long id,
            Pageable pageable) {
        Page<WebhookDeliveryLogResponse> page = webhookService.findDeliveryLogsByWebhookId(id, pageable);
        return ResponseEntity.ok(deliveryPagedAssembler.toModel(page));
    }

    private EntityModel<WebhookRegistrationResponse> toEntityModel(WebhookRegistrationResponse response) {
        return EntityModel.of(response,
                linkTo(methodOn(WebhookController.class).getById(response.getId())).withSelfRel(),
                linkTo(methodOn(WebhookController.class).getAll(null)).withRel("webhooks"));
    }
}
