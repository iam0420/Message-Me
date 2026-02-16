package com.chatapp.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

@Data
public class GroupCreateRequest {
    @NotBlank @Size(min = 1, max = 100)
    private String name;
    @Size(max = 500)
    private String description;
    @NotEmpty(message = "At least one member is required")
    private List<Long> memberIds;
}
