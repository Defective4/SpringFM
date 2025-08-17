package io.github.defective4.springfm.backend.config;

import java.util.List;
import java.util.Objects;

public class SpringFMModuleInfo {
    private final List<String> description;
    private final String name, version;

    public SpringFMModuleInfo(String name, String version, List<String> description) {
        this.name = Objects.requireNonNull(name);
        this.version = Objects.requireNonNull(version);
        this.description = description;
    }

    public List<String> getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

}
