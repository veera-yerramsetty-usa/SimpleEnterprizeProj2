package org.sample.simpleenterprizeproj2.controller;

import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final PagedResourcesAssembler<UserResponse> pagedAssembler;

    public UserController(UserService userService,
                          PagedResourcesAssembler<UserResponse> pagedAssembler) {
        this.userService = userService;
        this.pagedAssembler = pagedAssembler;
    }

    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<UserResponse>>> getAll(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String role,
            Pageable pageable) {
        Page<UserResponse> page = userService.findAll(username, email, role, pageable);
        return ResponseEntity.ok(pagedAssembler.toModel(page,
                response -> toEntityModel(response)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<UserResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(toEntityModel(userService.findResponseById(id)));
    }

    @PostMapping
    public ResponseEntity<EntityModel<UserResponse>> create(@Valid @RequestBody UserRequest request) {
        UserResponse response = userService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(response.getId()).toUri();
        return ResponseEntity.created(location).body(toEntityModel(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EntityModel<UserResponse>> update(
            @PathVariable Long id, @Valid @RequestBody UserRequest request) {
        return ResponseEntity.ok(toEntityModel(userService.update(id, request)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<EntityModel<UserResponse>> patch(
            @PathVariable Long id, @Valid @RequestBody UserPatchRequest request) {
        return ResponseEntity.ok(toEntityModel(userService.patch(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private EntityModel<UserResponse> toEntityModel(UserResponse response) {
        return EntityModel.of(response,
                linkTo(methodOn(UserController.class).getById(response.getId())).withSelfRel(),
                linkTo(methodOn(UserController.class).getAll(null, null, null, null)).withRel("users"));
    }
}
