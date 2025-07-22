package io.github.defective4.springfm.backend.exception;

import java.util.Objects;

public class ProfileNotFoundException extends RuntimeException {
    private final String invalidProfile;

    public ProfileNotFoundException(String invalidProfile) {
        this.invalidProfile = Objects.requireNonNull(invalidProfile);
    }

    public String getInvalidProfile() {
        return invalidProfile;
    }

}
