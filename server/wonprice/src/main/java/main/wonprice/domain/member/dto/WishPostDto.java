package main.wonprice.domain.member.dto;

import lombok.Getter;

import jakarta.validation.constraints.NotBlank;

@Getter
public class WishPostDto {

    @NotBlank
    private Long productId;
}
