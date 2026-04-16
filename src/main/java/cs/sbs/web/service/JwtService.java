package cs.sbs.web.service;

import cs.sbs.web.dto.TokenResponse;
import org.springframework.security.core.Authentication;

public interface JwtService {

    TokenResponse issueToken(Authentication authentication);

    Authentication parseToken(String token);
}

