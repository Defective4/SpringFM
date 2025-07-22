package io.github.defective4.springfm.backend.web;

import java.io.DataOutputStream;
import java.util.Map;
import java.util.function.Function;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import io.github.defective4.springfm.backend.exception.ProfileNotFoundException;
import io.github.defective4.springfm.backend.profile.RadioProfile;
import io.github.defective4.springfm.server.data.AuthResponse;
import io.github.defective4.springfm.server.data.ProfileInformation;
import io.github.defective4.springfm.server.data.ServiceInformation;
import io.github.defective4.springfm.server.service.RadioService;
import jakarta.servlet.http.HttpServletResponse;

@RestController
public class ProfileController {

    private final Map<String, RadioProfile> profiles;

    public ProfileController(Map<String, RadioProfile> profiles) {
        this.profiles = profiles;
    }

    @GetMapping(path = "/auth")
    public AuthResponse auth() {
        return new AuthResponse(profiles.entrySet().stream().map(profile -> new ProfileInformation(profile.getKey(),
                profile.getValue().getServices().stream().map(new Function<RadioService, ServiceInformation>() {
                    int index = 0;

                    @Override
                    public ServiceInformation apply(RadioService svc) {
                        return new ServiceInformation(index++, svc.getName());
                    }
                }).toList())).toList());
    }

    @ExceptionHandler(ProfileNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String profileNotFound(ProfileNotFoundException e) {
        return String.format("Sorry, profile \"%s\" was not found.", e.getInvalidProfile());
    }

    @GetMapping(path = "/profile/{profile}/stream")
    public StreamingResponseBody radioStream(@PathVariable String profile, HttpServletResponse resp) {
        RadioProfile prof = profiles.get(profile);
        if (prof == null) throw new ProfileNotFoundException(profile);
        resp.setContentType("application/octet-stream");
        return out -> {
            prof.addClient(new DataOutputStream(out));
            Object obj = new Object();
            synchronized (obj) {
                try {
                    obj.wait();
                } catch (InterruptedException e) {}
            }
        };
    }
}
