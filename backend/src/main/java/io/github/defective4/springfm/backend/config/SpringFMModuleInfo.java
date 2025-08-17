package io.github.defective4.springfm.backend.config;

import java.util.List;
import java.util.Objects;

import com.google.gson.annotations.SerializedName;

public class SpringFMModuleInfo {
    @SerializedName("name")
    private final String aname;

    @SerializedName("version")
    private final String bversion;
    private final List<String> description;

    public SpringFMModuleInfo(String name, String version, List<String> description) {
        aname = Objects.requireNonNull(name);
        bversion = Objects.requireNonNull(version);
        this.description = description;
    }

    public List<String> getDescription() {
        return description;
    }

    public String getName() {
        return aname;
    }

    public String getVersion() {
        return bversion;
    }

}
