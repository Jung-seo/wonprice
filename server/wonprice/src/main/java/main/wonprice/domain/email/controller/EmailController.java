package main.wonprice.domain.email.controller;

import main.wonprice.domain.email.dto.EmailAuthDto;
import main.wonprice.domain.email.service.EmailService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import java.io.UnsupportedEncodingException;

@RestController
@RequestMapping("/email")
public class EmailController {

    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/auth/send")
    public ResponseEntity sendAuthEmail(@RequestBody @Valid EmailAuthDto emailDto) throws MessagingException, UnsupportedEncodingException {

        String email = emailDto.getEmail();
        emailService.sendAuthEmail(email);

        return new ResponseEntity(HttpStatus.OK);
    }

    @PostMapping("/auth")
    public ResponseEntity veriftyAuthCode(@RequestBody @Valid EmailAuthDto emailDto) throws MessagingException, UnsupportedEncodingException {

        String email = emailDto.getEmail();
        String authCode = emailDto.getAuthCode();

        boolean result = emailService.verifyAuthCode(email, authCode);

        return result
                ? new ResponseEntity(HttpStatus.OK)
                : new ResponseEntity(HttpStatus.UNAUTHORIZED);
    }
}
