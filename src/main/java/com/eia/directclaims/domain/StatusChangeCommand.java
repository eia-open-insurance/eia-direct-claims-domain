package com.eia.directclaims.domain;

import jakarta.validation.constraints.NotBlank;

public record StatusChangeCommand(
        @NotBlank String status
) {
}
