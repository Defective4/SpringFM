package io.github.defective4.springfm.backend.config;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.sound.sampled.AudioFormat;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.github.defective4.springfm.backend.config.file.AudioFormatConfiguration;
import io.github.defective4.springfm.backend.config.file.ProfileConfiguration;
import io.github.defective4.springfm.backend.config.file.ServiceConfiguration;
import io.github.defective4.springfm.backend.config.file.UserConfiguration;
import io.github.defective4.springfm.backend.profile.RadioProfile;
import io.github.defective4.springfm.server.service.RadioService;
import io.github.defective4.springfm.server.service.ServiceArgument;
import io.github.defective4.springfm.server.service.impl.BroadcastFMService;

@Configuration
public class RadioConfiguration {

    private static final File CONFIG_FILE = new File("springfm.json");
    private final UserConfiguration config;
    private final Gson gson;
    private final Map<String, RadioProfile> profiles = new LinkedHashMap<>();

    public RadioConfiguration(Gson gson)
            throws IOException, ClassNotFoundException, NoSuchMethodException, SecurityException,
            IllegalAccessException, InvocationTargetException, InstantiationException, IllegalArgumentException {
        this.gson = gson;
        if (!CONFIG_FILE.exists()) saveDefaultConfig();
        try (Reader reader = new FileReader(CONFIG_FILE, StandardCharsets.UTF_8)) {
            config = gson.fromJson(reader, UserConfiguration.class);
        }
        for (Entry<String, ProfileConfiguration> entry : config.getProfiles().entrySet()) {
            String name = entry.getKey();
            ProfileConfiguration profileConfig = entry.getValue();
            List<RadioService> services = new ArrayList<>();
            for (ServiceConfiguration svcConfig : profileConfig.getServices()) {
                Class<? extends RadioService> serviceClass = (Class<? extends RadioService>) Class
                        .forName(svcConfig.getClassName().contains(".") ? svcConfig.getClassName()
                                : "io.github.defective4.springfm.server.service.impl." + svcConfig.getClassName());
                Constructor<?> constructor = serviceClass.getConstructors()[0];
                Parameter[] params = constructor.getParameters();
                Object[] args = new Object[params.length];
                for (int i = 0; i < params.length; i++) {
                    Parameter param = params[i];
                    if (param.getType() == AudioFormat.class) {
                        args[i] = svcConfig.getFormat().toAudioFormat();
                    } else {
                        ServiceArgument argInfo = param.getAnnotation(ServiceArgument.class);
                        Object val;
                        if (svcConfig.getArgs().containsKey(argInfo.name())
                                && param.getType().isInstance(svcConfig.getArgs().get(argInfo.name()))) {
                            val = svcConfig.getArgs().get(argInfo.name());
                        } else if (argInfo.defaultValue().isEmpty())
                            val = null;
                        else {
                            Method m = param.getType().getMethod("valueOf", String.class);
                            val = m.invoke(null, argInfo.defaultValue());
                        }
                        args[i] = val;
                    }
                }
                services.add((RadioService) constructor.newInstance(args));
            }
            profiles.put(name, new RadioProfile(services));
        }
    }

    public UserConfiguration getConfig() {
        return config;
    }

    public Gson getGson() {
        return gson;
    }

    @Bean
    Map<String, RadioProfile> getAvailableProfiles() {
        return Collections.unmodifiableMap(profiles);
    }

    private static void saveDefaultConfig() throws IOException {
        UserConfiguration config = new UserConfiguration(Map.of("default",
                new ProfileConfiguration(List.of(new ServiceConfiguration(BroadcastFMService.class.getSimpleName(),
                        Map.of("name", "Broadcast FM", "lowerFreq", 87e6f, "upperFreq", 108e6f),
                        new AudioFormatConfiguration(44.1e3f, 1))))));
        try (Writer writer = new FileWriter(CONFIG_FILE, StandardCharsets.UTF_8)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(config, writer);
        }
    }
}
