package main.wonprice.domain.email.service;

import main.wonprice.domain.member.service.MemberService;
import main.wonprice.exception.BusinessLogicException;
import main.wonprice.exception.ExceptionCode;
import main.wonprice.redis.RedisService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.time.Duration;

@Service
@Transactional
public class EmailService {

    private final JavaMailSender emailSender;
    private final TemplateEngine templateEngine;
    private final MemberService memberService;
    private final RedisService redisService;


    @Value("${ADMIN_EMAIL}")
    private String emailFrom;

    @Value("${mail.authorized}")
    private String authorizedString;

    public EmailService(JavaMailSender emailSender, TemplateEngine templateEngine, MemberService memberService, RedisService redisService) {
        this.emailSender = emailSender;
        this.templateEngine = templateEngine;
        this.memberService = memberService;
        this.redisService = redisService;
    }

//    인증 코드 랜덤 생성
    public String generateRandomCode() {

        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();

        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

        for (int i = 0; i < 10; i++) {
            int randomIndex = random.nextInt(chars.length());
            sb.append(chars.charAt(randomIndex));
        }

        return sb.toString();
    }

//    인증 이메일 전송
    public void sendAuthEmail(String recipient) throws MessagingException, UnsupportedEncodingException {

        String authCode = generateRandomCode();

        memberService.checkExistEmail(recipient);

        Context context = new Context();
        context.setVariable("email", recipient);
        context.setVariable("authCode", authCode);

        String message = templateEngine.process("email-auth", context);

        String title = "WonPrice 회원가입 이메일 인증";

        MimeMessage mimeMessage = emailSender.createMimeMessage();

        MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage);

        mimeMessageHelper.setSubject(title);
        mimeMessageHelper.setTo(recipient);
        mimeMessageHelper.setFrom("WonPrice");
        mimeMessageHelper.setText(message, true);

        saveAuthCode(recipient, authCode);

        emailSender.send(mimeMessage);
    }

    public void saveAuthCode(String recipient, String authCode) {

        String existCode = redisService.getValue(recipient);

        if (existCode != null) {
            redisService.deleteValue(existCode);
        }
        redisService.setValue(recipient, authCode, Duration.ofMinutes(5));
    }

//    이메일 인증 코드 검증
    public boolean verifyAuthCode(String email, String authCode) {

        String findCode = redisService.getValue(email);

        if (findCode.isBlank()) {
            throw new BusinessLogicException(ExceptionCode.EMAIL_NOT_FOUND);
        }

        if (authCode.equals(findCode)) {
            redisService.setValue(email, authCode + authorizedString, Duration.ofMinutes(3));
            return true;
        } else return false;
    }

//    회원 가입 전 이메일 인증 확인
    public void checkBeforeJoinMember(String email) {
        if (!redisService.getValue(email).contains(authorizedString)) {
            throw new BusinessLogicException(ExceptionCode.EMAIL_NOT_AUTHENTICATED);
        }
    }

//    생성된지 5분이 지난 인증코드 삭제
//    @Scheduled(fixedDelay = 5000)
//    public void deleteTimeOverEmail() {
//        List<AuthEmail> timeOverMail = emailAuthRepository.findAllByCreatedAtIsBefore(LocalDateTime.now().minusMinutes(5));
//
//        if (!timeOverMail.isEmpty()){
//            emailAuthRepository.deleteAll(timeOverMail);
//        }
//    }
}
