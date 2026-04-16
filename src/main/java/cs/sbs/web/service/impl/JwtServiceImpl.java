package cs.sbs.web.service.impl;

import cs.sbs.web.config.JwtProperties;
import cs.sbs.web.dto.TokenResponse;
import cs.sbs.web.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class JwtServiceImpl implements JwtService {

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtServiceImpl(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public TokenResponse issueToken(Authentication authentication) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(properties.getAccessTokenTtlSeconds());

        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        String token = Jwts.builder()
                .setIssuer(properties.getIssuer())
                .setSubject(authentication.getName())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiresAt))
                .claim("roles", roles)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        return new TokenResponse("Bearer", token, expiresAt);
    }

    @Override
    public Authentication parseToken(String token) {
        Jws<Claims> jws = Jwts.parserBuilder()
                .setSigningKey(key)
                .requireIssuer(properties.getIssuer())
                .build()
                .parseClaimsJws(token);

        Claims claims = jws.getBody();
        String username = claims.getSubject();
        List<GrantedAuthority> authorities = parseAuthorities(claims.get("roles"));
        return new UsernamePasswordAuthenticationToken(username, token, authorities);
    }

    private List<GrantedAuthority> parseAuthorities(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (Object item : list) {
            if (item == null) {
                continue;
            }
            String role = item.toString();
            authorities.add(new SimpleGrantedAuthority(role));
        }
        return authorities;
    }
}

