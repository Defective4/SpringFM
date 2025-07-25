package io.github.defective4.springfm.backend.web;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import io.github.defective4.springfm.backend.Main;
import io.github.defective4.springfm.backend.exception.ProfileNotFoundException;
import io.github.defective4.springfm.backend.profile.RadioProfile;
import io.github.defective4.springfm.server.data.AnalogTuningInformation;
import io.github.defective4.springfm.server.data.AuthResponse;
import io.github.defective4.springfm.server.data.DigitalTuningInformation;
import io.github.defective4.springfm.server.data.PlayerCommand;
import io.github.defective4.springfm.server.data.ProfileInformation;
import io.github.defective4.springfm.server.data.SerializableAudioFormat;
import io.github.defective4.springfm.server.data.ServiceInformation;
import io.github.defective4.springfm.server.packet.Packet;
import io.github.defective4.springfm.server.packet.impl.PlayerCommandPayload;
import io.github.defective4.springfm.server.service.AnalogRadioService;
import io.github.defective4.springfm.server.service.DigitalRadioService;
import io.github.defective4.springfm.server.service.RadioService;
import io.github.defective4.springfm.server.util.AudioUtils;

@RestController
public class ProfileController {

    private final Main main;

    private final Map<String, RadioProfile> profiles;

    public ProfileController(Map<String, RadioProfile> profiles, Main main) {
        this.profiles = profiles;
        this.main = main;
    }

    @GetMapping(path = "/profile/{profile}/audio")
    public ResponseEntity<StreamingResponseBody> audioStream(@PathVariable String profile) {
        RadioProfile prof = profiles.get(profile);
        if (prof == null) throw new ProfileNotFoundException(profile);

        return ResponseEntity.ok().contentType(MediaType.parseMediaType("audio/wav")).body(out -> {
            prof.addAudioClient(out);
            out.write(AudioUtils.createWavHeader(prof.getAudioFormat()));
            out.flush();
            Object lock = new Object();
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {}
            }
        });
    }

    @GetMapping(path = "/auth")
    public AuthResponse auth() {
        return new AuthResponse(profiles.entrySet().stream().map(profile -> new ProfileInformation(profile.getKey(),
                profile.getValue().getServices().stream().map(new Function<RadioService, ServiceInformation>() {
                    int index = 0;

                    @Override
                    public ServiceInformation apply(RadioService svc) {
                        boolean isDigital = svc instanceof DigitalRadioService;
                        return new ServiceInformation(index++, svc.getName(),
                                isDigital ? ServiceInformation.TUNING_TYPE_DIGITAL
                                        : ServiceInformation.TUNING_TYPE_ANALOG,
                                svc instanceof DigitalRadioService digital
                                        ? new DigitalTuningInformation(List.of(digital.getStations()))
                                        : null,
                                svc instanceof AnalogRadioService analog
                                        ? new AnalogTuningInformation(analog.getMinFrequency(),
                                                analog.getMaxFrequency(), analog.getFrequencyStep())
                                        : null);
                    }
                }).toList(), new SerializableAudioFormat(profile.getValue().getAudioFormat()))).toList(),
                "A SpringFM instance");
    }

    @GetMapping(path = "/profile/{profile}/data")
    public ResponseEntity<StreamingResponseBody> dataStream(@PathVariable String profile) {
        RadioProfile prof = profiles.get(profile);
        if (prof == null) throw new ProfileNotFoundException(profile);

        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(out -> {
            DataOutputStream os = new DataOutputStream(out);
            prof.addDataClient(os);
            new Packet(new PlayerCommandPayload(new PlayerCommand(PlayerCommand.COMMAND_CHANGE_SERVICE,
                    Integer.toString(prof.getCurrentService())))).toStream(os);
            if (prof.getCurrentService() >= 0) {
                RadioService svc = prof.getServices().get(prof.getCurrentService());
                if (svc instanceof DigitalRadioService digital) {
                    new Packet(new PlayerCommandPayload(new PlayerCommand(PlayerCommand.COMMAND_DIGITAL_TUNE,
                            Integer.toString(digital.getCurrentStation())))).toStream(os);
                }
            }
            os.flush();
            Object lock = new Object();
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {}
            }
        });
    }

    @PostMapping(path = "/profile/{profile}/tune/digital")
    public String digitalTune(@PathVariable String profile, @RequestParam int index)
            throws IllegalArgumentException, IOException {
        RadioProfile prof = profiles.get(profile);
        if (prof == null) throw new ProfileNotFoundException(profile);
        int svc = prof.getCurrentService();
        if (svc < 0) throw new IllegalStateException("This profile has no started service.");
        RadioService service = prof.getServices().get(svc);
        if (!(service instanceof DigitalRadioService digital))
            throw new IllegalStateException("This service does not support digital tuning.");
        if (digital.getCurrentStation() == index) return "Not changed";
        digital.tune(index);
        prof.broadcastPacket(new Packet(new PlayerCommandPayload(
                new PlayerCommand(PlayerCommand.COMMAND_DIGITAL_TUNE, Integer.toString(index)))));
        return "Ok";
    }

    @ExceptionHandler({ IllegalArgumentException.class, IllegalStateException.class })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String illegalArgument(Exception e) {
        return e.getMessage();
    }

    @ExceptionHandler(ProfileNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String profileNotFound(ProfileNotFoundException e) {
        return String.format("Sorry, profile \"%s\" was not found.", e.getInvalidProfile());
    }

    @ExceptionHandler(IOException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String serverError(IOException e) {
        e.printStackTrace();
        return "There was an error on the server side";
    }

    @PostMapping(path = "/profile/{profile}/service")
    public String setService(@PathVariable String profile, @RequestParam int index) throws IOException {
        RadioProfile prof = profiles.get(profile);
        if (prof == null) throw new ProfileNotFoundException(profile);
        if (prof.getCurrentService() == index) {
            return "Not changed";
        }
        prof.setActiveService(index);
        prof.haltServices();
        prof.startCurrentService();
        prof.broadcastPacket(new Packet(new PlayerCommandPayload(
                new PlayerCommand(PlayerCommand.COMMAND_CHANGE_SERVICE, Integer.toString(index)))));
        if (index >= 0) {
            RadioService service = prof.getServices().get(index);
            if (service instanceof DigitalRadioService digital) prof.broadcastPacket(
                    new Packet(new PlayerCommandPayload(new PlayerCommand(PlayerCommand.COMMAND_DIGITAL_TUNE,
                            Integer.toString(digital.getCurrentStation())))));
        }

        return "Ok";
    }
}
