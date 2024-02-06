package main.wonprice.domain.member.dto;

import lombok.Getter;

import jakarta.persistence.Lob;

@Getter
public class ReviewPatchDto {

    private String title;

    @Lob
    private String content;

    private Long score;
}
