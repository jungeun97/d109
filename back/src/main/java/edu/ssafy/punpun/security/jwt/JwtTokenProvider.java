package edu.ssafy.punpun.security.jwt;

import edu.ssafy.punpun.entity.Child;
import edu.ssafy.punpun.entity.Member;
import edu.ssafy.punpun.repository.ChildRepository;
import edu.ssafy.punpun.repository.MemberRepository;
import edu.ssafy.punpun.security.oauth2.PrincipalOAuth2UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey = "secretKey";
    private final Long accessTokenExpiredTime = 1000L * 60 * 60;    // 1시간
    private final Long refreshTokenExpiredTime = 1000L * 60 * 60 * 24;  // 24시간, 1일

    private final MemberRepository memberRepository;
    private final ChildRepository childRepository;

    private final PrincipalOAuth2UserService principalOAuth2UserService;

    @PostConstruct
    protected void init() {
        log.debug("[jwt init] JWTTokenprovider 내 secretKey 초기화 시작");
        secretKey = Base64.getEncoder().encodeToString(secretKey.getBytes(StandardCharsets.UTF_8));
        log.debug("[jwt init] JWTTokenprovider 내 secretKey 초기화 완료");
    }

    public String createTokenMember(Member member) {
        log.debug("[createToken] Member 토큰 생성 시작");
        Claims claims = Jwts.claims()
                .setSubject(member.getName());
        claims.put("id", member.getId());
        claims.put("name", member.getName());
        claims.put("email", member.getEmail());
        claims.put("role", member.getRole());
        if (member.getPhoneNumber() == null) {
            claims.put("phoneNumber", "NoNumber");
        } else {
            claims.put("phoneNumber", member.getPhoneNumber());
        }

        Date now = new Date();
        String token = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + accessTokenExpiredTime))
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();

        log.debug("[createToken] Member 토큰 생성 완료");
        return token;
    }

    public String createTokenChild(Child child) {
        log.debug("[createToken] Child 토큰 생성 시작");
        Claims claims = Jwts.claims()
                .setSubject(child.getName());
        claims.put("id", child.getId());
        claims.put("name", child.getName());
        claims.put("email", child.getEmail());
        claims.put("role", child.getRole());
        if (child.getPhoneNumber() == null) {
            claims.put("phoneNumber", "NoNumber");
        } else {
            claims.put("phoneNumber", child.getPhoneNumber());
        }

        Date now = new Date();
        String token = Jwts.builder()
                .setClaims(claims)
                .setIssuer("punpun")
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + accessTokenExpiredTime))
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();

        log.debug("[createToken] Child 토큰 생성 완료");
        return token;
    }

    public String createRefreshToken() {
        Date now = new Date();
        String token = Jwts.builder()
                .setIssuer("punpun")
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + refreshTokenExpiredTime))
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
        return token;
    }

    public String resolveToken(HttpServletRequest request, String tokenType) {
//        log.info("[resolveToken] HTTP 헤더에서 Token 값 추출");
        log.debug("[resolveToken] Cookie에서 Token 값 추출");
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(tokenType)) {
                    return cookie.getValue();
                }
            }
        }
        // Http 요청의 헤더를 파싱해 Bearer 토큰을 리턴함
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        } else {
            return request.getHeader("Authorization");
        }
    }

    public boolean validateToken(String token) {
        log.debug("[validateToken] 토큰 유효 체크 시작");

        try {
            Jws<Claims> claims = Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token);
            log.debug("[validateToken] 토큰 유효 체크 완료");

            return !claims.getBody().getExpiration().before(new Date());
        } catch (Exception e) {
            log.debug("[validateToken] 토큰 유효 체크 예외 발생");
            // log.info(e.getMessage());
            return false;
        }
    }

    public Authentication getAuthentication(String token) {
        log.debug("[getAuthentication] 토큰 인증 정보 조회 시작");
        String userEmail = this.getUserEmail(token);
        UserDetails userDetails = principalOAuth2UserService.loadUserByUsername(userEmail);
        log.debug("[getAuthentication] 토큰 인증 정보 조회 완료, UserDetail userEmail: {}", userDetails.getPassword());
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    public String getUserEmail(String token) {
        log.debug("[getUserEmail] 토큰 기반 회원 구별 정보 추출");
        String info = Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody().get("email").toString();
        log.info("[getUserEmail] 토큰 기반 회원 구별 정보 추출 완료, info : {}", info);
        return info;
    }
}
