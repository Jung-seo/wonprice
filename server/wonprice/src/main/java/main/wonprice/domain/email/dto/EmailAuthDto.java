package main.wonprice.domain.email.dto;

import lombok.Getter;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Getter
public class EmailAuthDto {

    @Email(message = "이메일을 정확히 입력해 주세요")
    @NotBlank(message = "이메일을 입력해 주세요")
    private String email;

    private String authCode;
}
