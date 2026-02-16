package org.sample.simpleenterprizeproj2.controller;

import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/v1/departments")
public class DepartmentController {

    private final DepartmentService departmentService;
    private final PagedResourcesAssembler<DepartmentResponse> pagedAssembler;

    public DepartmentController(DepartmentService departmentService,
                                PagedResourcesAssembler<DepartmentResponse> pagedAssembler) {
        this.departmentService = departmentService;
        this.pagedAssembler = pagedAssembler;
    }

    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<DepartmentResponse>>> getAll(
            @RequestParam(required = false) String name,
            Pageable pageable) {
        Page<DepartmentResponse> page = departmentService.findAll(name, pageable);
        return ResponseEntity.ok(pagedAssembler.toModel(page,
                response -> toEntityModel(response)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<DepartmentResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(toEntityModel(departmentService.findResponseById(id)));
    }

    @PostMapping
    public ResponseEntity<EntityModel<DepartmentResponse>> create(
            @Valid @RequestBody DepartmentRequest request) {
        DepartmentResponse response = departmentService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(response.getId()).toUri();
        return ResponseEntity.created(location).body(toEntityModel(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EntityModel<DepartmentResponse>> update(
            @PathVariable Long id, @Valid @RequestBody DepartmentRequest request) {
        return ResponseEntity.ok(toEntityModel(departmentService.update(id, request)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<EntityModel<DepartmentResponse>> patch(
            @PathVariable Long id, @Valid @RequestBody DepartmentPatchRequest request) {
        return ResponseEntity.ok(toEntityModel(departmentService.patch(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        departmentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private EntityModel<DepartmentResponse> toEntityModel(DepartmentResponse response) {
        return EntityModel.of(response,
                linkTo(methodOn(DepartmentController.class).getById(response.getId())).withSelfRel(),
                linkTo(methodOn(DepartmentController.class).getAll(null, null)).withRel("departments"));
    }
}
