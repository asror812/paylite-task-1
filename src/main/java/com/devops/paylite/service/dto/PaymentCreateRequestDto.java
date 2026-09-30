package com.devops.paylite.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class PaymentCreateRequestDto {

    @NotBlank
    private String account;

    @Positive
    @NotNull
    private Long amount;
}
