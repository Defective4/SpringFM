package io.github.defective4.springfm.backend.config.file;

import java.util.Map;

import com.google.gson.annotations.SerializedName;

public class ServiceConfiguration {
    @SerializedName("className")
    private final String aclassName;

    @SerializedName("format")
    private final AudioFormatConfiguration bformat;

    @SerializedName("args")
    private final Map<String, Object> cargs;

    public ServiceConfiguration(String className, Map<String, Object> args, AudioFormatConfiguration format) {
        aclassName = className;
        cargs = args;
        bformat = format;
    }

    public Map<String, Object> getArgs() {
        return cargs;
    }

    public String getClassName() {
        return aclassName;
    }

    public AudioFormatConfiguration getFormat() {
        return bformat;
    }

    @Override
    public String toString() {
        return "ServiceConfiguration [args=" + cargs + ", className=" + aclassName + "]";
    }

}
