package cs.sbs.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@AllArgsConstructor
@Getter
public class TokenResponse {

    private final String tokenType;
    private final String accessToken;
    private final Instant expiresAt;
}

