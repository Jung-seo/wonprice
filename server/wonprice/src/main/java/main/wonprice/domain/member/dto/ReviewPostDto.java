package main.wonprice.domain.member.dto;

import lombok.Getter;

import jakarta.validation.constraints.NotBlank;

@Getter
public class ReviewPostDto {

    @NotBlank
    private String title;

    @NotBlank
    private String content;

    @NotBlank
    private Long score;

    @NotBlank
    private Long productId;
}
