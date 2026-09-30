package com.gymflow.auth.domain.port;

import java.time.Duration;

import com.gymflow.auth.domain.model.AccessToken;
import com.gymflow.auth.domain.model.AppUser;

public interface TokenIssuer {

    AccessToken issueAccessToken(AppUser user);

    Duration refreshTokenTtl();
}
