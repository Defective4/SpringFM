package io.github.defective4.springfm.backend.config.file;

import java.util.Map;

public class MainConfiguration {
    private final boolean debug;
    private final Map<String, ProfileConfiguration> profiles;

    public MainConfiguration(Map<String, ProfileConfiguration> profiles, boolean debug) {
        this.profiles = profiles;
        this.debug = debug;
    }

    public Map<String, ProfileConfiguration> getProfiles() {
        return profiles;
    }

    public boolean isDebug() {
        return debug;
    }

    @Override
    public String toString() {
        return "UserConfiguration [profiles=" + profiles + "]";
    }

}
