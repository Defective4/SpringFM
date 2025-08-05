package io.github.defective4.springfm.backend.config.file;

import java.util.Map;

public class UserConfiguration {
    private final Map<String, ProfileConfiguration> profiles;

    public UserConfiguration(Map<String, ProfileConfiguration> profiles) {
        this.profiles = profiles;
    }

    public Map<String, ProfileConfiguration> getProfiles() {
        return profiles;
    }

    @Override
    public String toString() {
        return "UserConfiguration [profiles=" + profiles + "]";
    }

}
