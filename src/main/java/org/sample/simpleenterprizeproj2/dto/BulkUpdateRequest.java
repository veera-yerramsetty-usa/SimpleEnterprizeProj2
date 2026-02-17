package org.sample.simpleenterprizeproj2.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public class BulkUpdateRequest<T> {

    @NotNull(message = "ID is required")
    private Long id;

    @NotNull(message = "Data is required")
    @Valid
    private T data;

    public BulkUpdateRequest() {}

    public BulkUpdateRequest(Long id, T data) {
        this.id = id;
        this.data = data;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}
