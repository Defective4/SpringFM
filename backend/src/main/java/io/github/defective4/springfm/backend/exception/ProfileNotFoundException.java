package io.github.defective4.springfm.backend.exception;

import java.util.Objects;

import io.javalin.http.Context;

public class ProfileNotFoundException extends RuntimeException {
    private final String invalidProfile;

    public ProfileNotFoundException(Context ctx) {
        invalidProfile = Objects.requireNonNull(ctx).pathParam("profile");
    }

    public String getInvalidProfile() {
        return invalidProfile;
    }

}
