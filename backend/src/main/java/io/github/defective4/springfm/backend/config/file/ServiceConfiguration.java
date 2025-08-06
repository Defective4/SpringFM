package io.github.defective4.springfm.backend.config.file;

import java.util.Map;

public class ServiceConfiguration {
    private final Map<String, Object> args;
    private final String className;
    private final AudioFormatConfiguration format;

    public ServiceConfiguration(String className, Map<String, Object> args, AudioFormatConfiguration format) {
        this.className = className;
        this.args = args;
        this.format = format;
    }

    public Map<String, Object> getArgs() {
        return args;
    }

    public String getClassName() {
        return className;
    }

    public AudioFormatConfiguration getFormat() {
        return format;
    }

    @Override
    public String toString() {
        return "ServiceConfiguration [args=" + args + ", className=" + className + "]";
    }

}
