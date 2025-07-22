package io.github.defective4.springfm.backend.config;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.defective4.springfm.backend.profile.RadioProfile;
import io.github.defective4.springfm.server.service.TestService;

@Configuration
public class RadioConfiguration {

    @Bean
    Map<String, RadioProfile> getAvailableProfiles() throws IOException {
        RadioProfile def = new RadioProfile(List.of(new TestService(new File("wbfm.wav"))));
        def.setActiveService(0);
        return Map.of("default", def);
    }
}
