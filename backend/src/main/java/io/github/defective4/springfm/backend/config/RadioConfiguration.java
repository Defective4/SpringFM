package io.github.defective4.springfm.backend.config;

import java.util.List;
import java.util.Map;

import javax.sound.sampled.AudioFormat;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.defective4.springfm.backend.profile.RadioProfile;
import io.github.defective4.springfm.server.service.impl.BroadcastFMService;

@Configuration
public class RadioConfiguration {

    @Bean
    Map<String, RadioProfile> getAvailableProfiles() {
        RadioProfile def = new RadioProfile(
                List.of(new BroadcastFMService("Broadcast FM", 87e6f, 108e6f, "rtl_tcp=127.0.0.1:55555", true, -1)),
                new AudioFormat(171e3f, 16, 1, true, false));
        return Map.of("default", def);
    }
}
