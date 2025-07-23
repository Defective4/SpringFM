package io.github.defective4.springfm.backend.config;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.defective4.springfm.backend.profile.RadioProfile;
import io.github.defective4.springfm.server.service.impl.TestService;

@Configuration
public class RadioConfiguration {

    @Bean
    Map<String, RadioProfile> getAvailableProfiles() {
        RadioProfile def = new RadioProfile(List.of(new TestService(new File("wbfm.wav"), "RMF FM"),
                new TestService(new File("wbfm2.wav"), "Jedynka")));
        return Map.of("default", def);
    }
}
