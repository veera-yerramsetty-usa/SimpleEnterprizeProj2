package org.sample.simpleenterprizeproj2.controller;

import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/v1/employees")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final PagedResourcesAssembler<EmployeeResponse> pagedAssembler;

    public EmployeeController(EmployeeService employeeService,
                              PagedResourcesAssembler<EmployeeResponse> pagedAssembler) {
        this.employeeService = employeeService;
        this.pagedAssembler = pagedAssembler;
    }

    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<EmployeeResponse>>> getAll(
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Long departmentId,
            Pageable pageable) {
        Page<EmployeeResponse> page = employeeService.findAll(firstName, lastName, email, departmentId, pageable);
        return ResponseEntity.ok(pagedAssembler.toModel(page,
                response -> toEntityModel(response)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<EmployeeResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(toEntityModel(employeeService.findResponseById(id)));
    }

    @PostMapping
    public ResponseEntity<EntityModel<EmployeeResponse>> create(
            @Valid @RequestBody EmployeeRequest request) {
        EmployeeResponse response = employeeService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(response.getId()).toUri();
        return ResponseEntity.created(location).body(toEntityModel(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EntityModel<EmployeeResponse>> update(
            @PathVariable Long id, @Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.ok(toEntityModel(employeeService.update(id, request)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<EntityModel<EmployeeResponse>> patch(
            @PathVariable Long id, @Valid @RequestBody EmployeePatchRequest request) {
        return ResponseEntity.ok(toEntityModel(employeeService.patch(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        employeeService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private EntityModel<EmployeeResponse> toEntityModel(EmployeeResponse response) {
        return EntityModel.of(response,
                linkTo(methodOn(EmployeeController.class).getById(response.getId())).withSelfRel(),
                linkTo(methodOn(EmployeeController.class).getAll(null, null, null, null, null)).withRel("employees"));
    }
}
