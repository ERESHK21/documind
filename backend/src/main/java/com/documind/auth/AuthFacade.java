package com.documind.auth;

import com.documind.user.User;
import org.springframework.stereotype.Component;

@Component
public class AuthFacade {

    public User current() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User user)) {
            throw com.documind.common.ApiException.unauthorized("Sign in to continue");
        }
        return user;
    }
}
