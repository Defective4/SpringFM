package io.github.defective4.springfm.backend.config;

import java.io.File;
import java.util.List;
import java.util.Map;

import javax.sound.sampled.AudioFormat;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.defective4.springfm.backend.profile.RadioProfile;
import io.github.defective4.springfm.server.service.impl.TestAnalogService;
import io.github.defective4.springfm.server.service.impl.TestDigitalService;

@Configuration
public class RadioConfiguration {

    @Bean
    Map<String, RadioProfile> getAvailableProfiles() {
        RadioProfile def = new RadioProfile(
                List.of(new TestDigitalService(new File[] { new File("wbfm.wav"), new File("wbfm2.wav") }, "RMF FM"),
                        new TestAnalogService(new File("wbfm2.wav"), "Jedynka")),
                new AudioFormat(171e3f, 16, 1, true, false));
        return Map.of("default", def);
    }
}
