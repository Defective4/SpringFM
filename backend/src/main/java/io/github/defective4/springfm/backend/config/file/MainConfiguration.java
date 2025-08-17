package io.github.defective4.springfm.backend.config.file;

import java.util.Collections;
import java.util.Map;

public class MainConfiguration {
    private final boolean debug;
    private final String moduleDirectory;
    private final Map<String, ProfileConfiguration> profiles;
    private final ServerConfiguration server;

    public MainConfiguration(Map<String, ProfileConfiguration> profiles, boolean debug, ServerConfiguration server,
            String moduleDirectory) {
        this.profiles = profiles;
        this.debug = debug;
        this.server = server;
        this.moduleDirectory = moduleDirectory;
    }

    public String getModuleDirectory() {
        return moduleDirectory == null ? "modules" : moduleDirectory;
    }

    public Map<String, ProfileConfiguration> getProfiles() {
        return profiles == null ? Collections.emptyMap() : profiles;
    }

    public boolean isDebug() {
        return debug;
    }

    @Override
    public String toString() {
        return "MainConfiguration [debug=" + debug + ", profiles=" + profiles + ", server=" + server
                + ", moduleDirectory=" + moduleDirectory + "]";
    }

}
