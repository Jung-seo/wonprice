package main.wonprice.domain.member.dto;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Lob;
import java.time.LocalDateTime;

@Getter @Setter
public class ReviewResponseDto {

    private Long postMemberId;

    private Long targetMemberId;

    @Lob
    private String content;

    private Long score;

    private LocalDateTime createdAt;
}