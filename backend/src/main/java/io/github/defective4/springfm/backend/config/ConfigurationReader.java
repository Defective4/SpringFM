package io.github.defective4.springfm.backend.config;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.sound.sampled.AudioFormat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.github.defective4.springfm.backend.config.file.AudioFormatConfiguration;
import io.github.defective4.springfm.backend.config.file.MainConfiguration;
import io.github.defective4.springfm.backend.config.file.ProfileConfiguration;
import io.github.defective4.springfm.backend.config.file.ServerConfiguration;
import io.github.defective4.springfm.backend.config.file.ServiceConfiguration;
import io.github.defective4.springfm.backend.profile.RadioProfile;
import io.github.defective4.springfm.server.service.RadioService;
import io.github.defective4.springfm.server.service.ServiceArgument;
import io.github.defective4.springfm.server.service.impl.AMService;
import io.github.defective4.springfm.server.service.impl.BroadcastFMService;
import io.github.defective4.springfm.server.service.impl.NarrowbandFMService;
import io.javalin.json.JsonMapper;

public class ConfigurationReader {

    private static final File CONFIG_FILE = new File("springfm.json");
    private final MainConfiguration config;
    private final Gson gson = new Gson();
    private ClassLoader moduleLoader;
    private List<SpringFMModule> modules;
    private final Map<String, RadioProfile> profiles = new LinkedHashMap<>();

    public ConfigurationReader() throws IOException, ClassNotFoundException, NoSuchMethodException, SecurityException,
            IllegalAccessException, InvocationTargetException, InstantiationException, IllegalArgumentException {
        if (!CONFIG_FILE.exists()) saveDefaultConfig();
        try (Reader reader = new FileReader(CONFIG_FILE, StandardCharsets.UTF_8)) {
            config = gson.fromJson(reader, MainConfiguration.class);
        }

        System.err.println("Loading modules...");
        File modulesDir = new File(config.getModuleDirectory());

        modules = Arrays.stream(modulesDir.listFiles()).filter(mod -> mod.getName().toLowerCase().endsWith(".jar"))
                .map(file -> {
                    try (ZipFile zipFile = new ZipFile(file)) {
                        ZipEntry entry = zipFile.getEntry("springfm-module.json");
                        if (entry == null || entry.isDirectory())
                            throw new IOException("springfm-module.json does not exist in the module archive");
                        SpringFMModuleInfo info;
                        try (Reader reader = new InputStreamReader(zipFile.getInputStream(entry))) {
                            info = gson.fromJson(reader, SpringFMModuleInfo.class);
                        }
                        URL url = file.toURI().toURL();
                        SpringFMModule mod = new SpringFMModule(url, info);
                        System.err.println("Discovered module \"" + info.getName() + "\"");
                        return mod;
                    } catch (Exception e) {
                        System.err.println("Couldn't load module at " + file.getName());
                        e.printStackTrace();
                        return null;
                    }
                }).toList();
        System.err.println("Discovered " + modules.size() + " modules.");

        moduleLoader = new URLClassLoader(modules.stream().map(mod -> mod.getUrl()).toList().toArray(new URL[0]),
                getClass().getClassLoader());

        System.err.println("Loading services...");
        for (Entry<String, ProfileConfiguration> entry : config.getProfiles().entrySet()) {
            String name = entry.getKey();
            ProfileConfiguration profileConfig = entry.getValue();
            List<RadioService> services = new ArrayList<>();
            for (ServiceConfiguration svcConfig : profileConfig.getServices()) {
                Class<? extends RadioService> serviceClass = (Class<? extends RadioService>) Class.forName(
                        svcConfig.getClassName().contains(".") ? svcConfig.getClassName()
                                : "io.github.defective4.springfm.server.service.impl." + svcConfig.getClassName(),
                        true, moduleLoader);
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

                        if (param.getType().isEnum() && svcConfig.getArgs().containsKey(argInfo.name())
                                && svcConfig.getArgs().get(argInfo.name()) instanceof String str) {
                            Method m = param.getType().getMethod("valueOf", String.class);
                            val = m.invoke(null, str.toUpperCase());
                        } else if (svcConfig.getArgs().containsKey(argInfo.name())
                                && param.getType().isInstance(svcConfig.getArgs().get(argInfo.name()))) {
                            val = svcConfig.getArgs().get(argInfo.name());
                        } else if (argInfo.defaultValue().isEmpty()) {
                            val = null;
                        } else if (param.getType() == String.class) {
                            val = argInfo.defaultValue();
                        } else {
                            Method m = param.getType().getMethod("valueOf", String.class);
                            val = m.invoke(null, argInfo.defaultValue());
                        }
                        args[i] = val;
                    }
                }
                RadioService service = (RadioService) constructor.newInstance(args);
                System.err.println("Instantiated service \"" + service.getName() + "\" ("
                        + service.getClass().getSimpleName() + "), checking its dependencies...");
                Collection<String> missing = service.checkMissingDependencies();
                if (!missing.isEmpty()) {
                    System.err.println("Couldn't create service \"" + service.getName()
                            + "\", because some of its dependencies are missing or misconfigured.");
                    System.err.println("Missing dependencies:");
                    missing.forEach(p -> System.err.println(" - " + p));
                    System.err.println("Install or configure the dependencies and try again.");
                    System.exit(7);
                    return;
                }
                System.err.println("Initializing service \"" + service.getName() + "\"");
                try {
                    service.init();
                } catch (Exception e) {
                    System.err.println("Couldn't initialize service \"" + service.getName() + "\"");
                    e.printStackTrace();
                    System.exit(8);
                    return;
                }
                service.setDebugMode(config.isDebug());
                boolean b = services.add(service);
            }
            profiles.put(name, new RadioProfile(services));
            System.err.println("Loaded " + profiles.size() + " profiles with "
                    + profiles.values().stream().mapToInt(p -> p.getServices().size()).sum() + " total services.");
        }

    }

    public Map<String, RadioProfile> getAvailableProfiles() {
        return Collections.unmodifiableMap(profiles);
    }

    public MainConfiguration getConfig() {
        return config;
    }

    public Gson getGson() {
        return gson;
    }

    public JsonMapper getGsonMapper() {
        return new JsonMapper() {

            private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

            @Override
            public <T> T fromJsonString(String json, Type targetType) {
                return gson.fromJson(json, targetType);
            }

            @Override
            public String toJsonString(Object obj, Type type) {
                return gson.toJson(obj, type);
            }

        };
    }

    public MessageDigest getMessageDigest() throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("md5");
    }

    public List<SpringFMModule> getModules() {
        return modules;
    }

    private static void saveDefaultConfig() throws IOException {
        MainConfiguration config = new MainConfiguration(
                Map.of("default",
                        new ProfileConfiguration(List.of(
                                new ServiceConfiguration(BroadcastFMService.class.getSimpleName(),
                                        Map.of("name", "Broadcast FM", "lowerFrequency", 87e6f, "upperFrequency",
                                                108e6f),
                                        new AudioFormatConfiguration(44.1e3f, 1)),
                                new ServiceConfiguration(NarrowbandFMService.class.getSimpleName(),
                                        Map.of("name", "Narrowband FM (2m)", "lowerFrequency", 137e6f, "upperFrequency",
                                                150e6f, "frequencyStep", 5e3f),
                                        new AudioFormatConfiguration(44.1e3f, 1)),
                                new ServiceConfiguration(AMService.class.getSimpleName(),
                                        Map.of("name", "Broadcast AM", "lowerFrequency", 1e6f, "upperFrequency", 10e6f,
                                                "frequencyStep", 1e3f),
                                        new AudioFormatConfiguration(44.1e3f, 1))))),
                false, new ServerConfiguration("0.0.0.0", 8080), "modules");
        try (Writer writer = new FileWriter(CONFIG_FILE, StandardCharsets.UTF_8)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(config, writer);
        }
    }
}
