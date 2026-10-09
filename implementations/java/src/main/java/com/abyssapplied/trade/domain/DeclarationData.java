package com.abyssapplied.trade.domain;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Business input. Integer cents avoid floating-point money storage. */
public record DeclarationData(
    @NotBlank @Size(max = 120) String importer,
    @NotBlank @Pattern(regexp = "[A-Z]{2}") String originCountry,
    @NotBlank @Pattern(regexp = "[0-9]{6}") String commodityCode,
    @Min(1) @Max(100000000000L) long declaredValueCents,
    @NotBlank @Pattern(regexp = "USD") String currency,
    @NotBlank @Size(max = 80) String shipmentReference) {}
