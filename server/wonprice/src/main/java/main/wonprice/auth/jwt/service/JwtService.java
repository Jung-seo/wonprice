package main.wonprice.auth.jwt.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import main.wonprice.domain.member.entity.Member;
import main.wonprice.domain.member.service.MemberService;
import main.wonprice.exception.BusinessLogicException;
import main.wonprice.exception.ExceptionCode;
import main.wonprice.redis.RedisService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.util.*;

@Getter
@Service
@Transactional
@Slf4j
public class JwtService {

    @Value("${jwt.key}")
    private String secretKey;

    @Getter
    @Value("${jwt.access-token-expiration-minutes}")
    private int accessTokenExpirationMinutes;

    @Getter
    @Value("${jwt.refresh-token-expiration-minutes}")
    private int refreshTokenExpirationMinutes;

    private final MemberService memberService;
    private final RedisService redisService;

    public JwtService(MemberService memberService, RedisService redisService) {
        this.memberService = memberService;
        this.redisService = redisService;
    }

    //    refresh 토큰 발급 시 DB에 저장
    public void saveRefreshToken(Member member, String refresh) {

        String redisKey = redisService.getRefreshTokenKeyForUser(member.getMemberId());
        String token = redisService.getValue(redisKey);

        if (token != null) {
            redisService.deleteValue(redisKey);
        }
        redisService.setValue(redisKey, refresh, Duration.ofMinutes(refreshTokenExpirationMinutes));
    }

//    refresh 토큰으로 access 토큰 재발급
    public String generateNewAccessToken(HttpServletRequest request) {

        String refreshToken = request.getHeader("Refresh");

//        refresh 토큰 검증
        String email = getClaims(refreshToken, encodeBase64SecretKey(getSecretKey())).getBody().getSubject();
        Member member = memberService.findMember(email);

        String redisKey = redisService.getRefreshTokenKeyForUser(member.getMemberId());
        String findToken = redisService.getValue(redisKey);

        if (findToken == null) {
            log.info("receive null refresh-token when try generate new access-token");
            throw new BusinessLogicException(ExceptionCode.INVALID_TOKEN);
        }

        if (!findToken.equals(refreshToken)) {
            throw new BusinessLogicException(ExceptionCode.INVALID_TOKEN);
        }

        return generateAccessToken(member);
    }

    /*
    상시 로그인 상태 확인 시 access와 refresh를 같이 보내고 둘다 검증함
    중복 로그인 시 앞서 로그인 한 사람의 refresh 토큰을 삭제

    토큰 만료시 401
    시그니처 오류 시 406
    refresh가 뒤이어 로그인한 사람의 것과 다르거나 없을때 406
    */
    public void verifyTokens(HttpServletRequest request) {

        String requestAccess = request.getHeader("Authorization").replace("Bearer ", "");
        String requestRefresh = request.getHeader("Refresh");

        String base64EncodedSecretKey = encodeBase64SecretKey(getSecretKey());

        /*
        access 검증
        JWT에서 Claims를 parsing 할 수 있다 -> 내부적으로 Signature 검증에 성공했다
        */
        String email = getClaims(requestAccess, base64EncodedSecretKey).getBody().getSubject();
        Member member = memberService.findMember(email);

        String redisKey = redisService.getRefreshTokenKeyForUser(member.getMemberId());
        String refreshToken = redisService.getValue(redisKey);

        if (refreshToken == null) {
            log.info("receive null token");
            throw new BusinessLogicException(ExceptionCode.INVALID_TOKEN);
        }
        log.info(requestRefresh);

        if (!requestRefresh.equals(refreshToken)) {
            log.info("different token receive");
            throw new BusinessLogicException(ExceptionCode.INVALID_TOKEN);
        }
    }

    //    Plain Text 형태인 Secret Key의 byte[]를 Base64 형식의 문자열로 인코딩
    public String encodeBase64SecretKey(String secretKey) {
        return Encoders.BASE64.encode(secretKey.getBytes(StandardCharsets.UTF_8));
    }

//    access 토큰 생성
    public String generateAccessToken(Member member) {

        Map<String, Object> claims = new HashMap<>();
        claims.put("email", member.getEmail());
        claims.put("roles", member.getRoles());

        String subject = member.getEmail();
        Date expiration = getTokenExpiration(getAccessTokenExpirationMinutes());
        String base64EncodedSecretKey = encodeBase64SecretKey(getSecretKey());
        Key key = getKeyFromBase64EncodedKey(base64EncodedSecretKey);

        return Jwts.builder()
                .setClaims(claims)                             // 인증된 사용자와 관련된 정보
                .setSubject(subject)                           // JWT에 대한 제목
                .setIssuedAt(Calendar.getInstance().getTime()) // 발행 일자
                .setExpiration(expiration)                     // 만료 일시
                .signWith(key)                                 // 서명
                .compact();                                    // JWT 생성 및 직렬화
    }

//    refresh 토큰 생성
    public String generateRefreshToken(Member member) {

        String subject = member.getEmail();
        Date expiration = getTokenExpiration(getRefreshTokenExpirationMinutes());
        String base64EncodedSecretKey = encodeBase64SecretKey(getSecretKey());
        Key key = getKeyFromBase64EncodedKey(base64EncodedSecretKey);

        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(Calendar.getInstance().getTime())
                .setExpiration(expiration)
                .signWith(key)
                .compact();
    }

    //    시그니처 파싱 및 상황별 예외처리
    public Jws<Claims> getClaims(String jws, String base64EncodedSecretKey) {
        Key key = getKeyFromBase64EncodedKey(base64EncodedSecretKey);

        Jws<Claims> claims = null;
        try {
            claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(jws);
        } catch (ExpiredJwtException e) {
            throw new BusinessLogicException(ExceptionCode.MEMBER_NOT_AUTHENTICATED);
        } catch (UnsupportedJwtException e) {
            throw new RuntimeException(e);
        } catch (MalformedJwtException e) {
            throw new RuntimeException(e);
//        } catch (SignatureException e) {
//            log.info("token SignatureException");
//            throw new BusinessLogicException(ExceptionCode.INVALID_TOKEN);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
        return claims;
    }

    public void verifySignature(String jws, String base64EncodedSecretKey) {

        Key key = getKeyFromBase64EncodedKey(base64EncodedSecretKey);

        Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(jws);
    }

    public Date getTokenExpiration(int expirationMinutes) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, expirationMinutes);
        Date expiration = calendar.getTime();

        return expiration;
    }

    private Key getKeyFromBase64EncodedKey(String base64EncodedSecretKey) {
        byte[] keyBytes = Decoders.BASE64.decode(base64EncodedSecretKey);
        Key key = Keys.hmacShaKeyFor(keyBytes);

        return key;
    }

    /*
    로그인이 되어 있는지
    되어 있다면 들어온 토큰 값은 유효한 지 검증
    */
    public boolean isLogin(String accessToken) {

        if (accessToken == null) {
            return false;
        }
        try { // 토큰 검증
            String jws = accessToken.replace("Bearer ", "");
            verifySignature(jws, encodeBase64SecretKey(getSecretKey()));
        } catch (BusinessLogicException be) {
            return false;
        }
        return true;
    }
}
