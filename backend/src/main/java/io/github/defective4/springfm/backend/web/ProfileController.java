package io.github.defective4.springfm.backend.web;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.sound.sampled.AudioFormat;

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
import io.github.defective4.springfm.server.data.GainInformation;
import io.github.defective4.springfm.server.data.PlayerCommand;
import io.github.defective4.springfm.server.data.ProfileInformation;
import io.github.defective4.springfm.server.data.SerializableAudioFormat;
import io.github.defective4.springfm.server.data.ServiceInformation;
import io.github.defective4.springfm.server.packet.Packet;
import io.github.defective4.springfm.server.packet.impl.PlayerCommandPayload;
import io.github.defective4.springfm.server.service.AdjustableGainService;
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

    @PostMapping(path = "/profile/{profile}/gain")
    public String adjustGain(@PathVariable String profile, @RequestParam float gain)
            throws IllegalArgumentException, IOException {
        RadioProfile prof = getProfile(profile);
        RadioService service = getCurrentService(prof);
        if (service instanceof AdjustableGainService adjustable) {
            if (adjustable.getCurrentGain() == gain) return "Not changed";
            adjustable.setGain(gain);
            prof.broadcastPacket(new Packet(new PlayerCommandPayload(
                    new PlayerCommand(PlayerCommand.COMMAND_ADJUST_GAIN, Float.toString(gain)))));
            return "Ok";
        }
        throw new IllegalStateException("This service does not support gain adjusting");
    }

    @PostMapping(path = "/profile/{profile}/tune/analog")
    public String analogTune(@PathVariable String profile, @RequestParam int frequency)
            throws IllegalArgumentException, IOException {
        RadioProfile prof = getProfile(profile);
        RadioService service = getCurrentService(prof);
        if (service instanceof AnalogRadioService analog) {
            float absoluteFreq = frequency * analog.getFrequencyStep();
            if (analog.getCurrentFrequency() == absoluteFreq) return "Not changed";
            analog.tune(absoluteFreq);
            prof.broadcastPacket(new Packet(new PlayerCommandPayload(
                    new PlayerCommand(PlayerCommand.COMMAND_ANALOG_TUNE, Integer.toString(frequency)))));
            return "Ok";
        }
        throw new IllegalStateException("This service does not support analog tuning.");
    }

    @GetMapping(path = "/profile/{profile}/audio")
    public ResponseEntity<StreamingResponseBody> audioStream(@PathVariable String profile) {
        RadioProfile prof = getProfile(profile);
        int index = prof.getCurrentService();
        AudioFormat fmt;
        if (index < 0) {
            fmt = new AudioFormat(44.1e3f, 16, 1, true, false);
        } else {
            fmt = prof.getServices().get(index).getAudioFormat();
        }

        return ResponseEntity.ok().contentType(MediaType.parseMediaType("audio/wav")).body(out -> {
            DataOutputStream dout = new DataOutputStream(out);
            dout.write(AudioUtils.createWavHeader(fmt));
            SerializableAudioFormat.Codec.toStream(new SerializableAudioFormat(fmt), dout);
            dout.flush();
            prof.addAudioClient(out);
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
                        GainInformation gainInfo;
                        if (svc instanceof AdjustableGainService adjustable)
                            gainInfo = new GainInformation(adjustable.getMaxGain(), true);
                        else
                            gainInfo = new GainInformation(0, false);
                        return new ServiceInformation(index++, svc.getName(),
                                isDigital ? ServiceInformation.TUNING_TYPE_DIGITAL
                                        : ServiceInformation.TUNING_TYPE_ANALOG,
                                svc instanceof DigitalRadioService digital
                                        ? new DigitalTuningInformation(List.of(digital.getStations()))
                                        : null,
                                svc instanceof AnalogRadioService analog
                                        ? new AnalogTuningInformation(analog.getMinFrequency(),
                                                analog.getMaxFrequency(), analog.getFrequencyStep())
                                        : null,
                                gainInfo);
                    }
                }).toList())).toList(), "A SpringFM instance");
    }

    @GetMapping(path = "/profile/{profile}/data")
    public ResponseEntity<StreamingResponseBody> dataStream(@PathVariable String profile) {
        RadioProfile prof = getProfile(profile);

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
                } else if (svc instanceof AnalogRadioService analog) {
                    new Packet(new PlayerCommandPayload(new PlayerCommand(PlayerCommand.COMMAND_ANALOG_TUNE,
                            Integer.toString((int) (analog.getCurrentFrequency() / analog.getFrequencyStep())))))
                            .toStream(os);
                }

                if (svc instanceof AdjustableGainService adjustable) {
                    prof.broadcastPacket(
                            new Packet(new PlayerCommandPayload(new PlayerCommand(PlayerCommand.COMMAND_ADJUST_GAIN,
                                    Float.toString(adjustable.getCurrentGain())))));
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
        RadioProfile prof = getProfile(profile);
        RadioService service = getCurrentService(prof);
        if (service instanceof DigitalRadioService digital) {
            if (digital.getCurrentStation() == index) return "Not changed";
            digital.tune(index);
            prof.broadcastPacket(new Packet(new PlayerCommandPayload(
                    new PlayerCommand(PlayerCommand.COMMAND_DIGITAL_TUNE, Integer.toString(index)))));
            return "Ok";
        }
        throw new IllegalStateException("This service does not support digital tuning.");
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
        RadioProfile prof = getProfile(profile);
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
            if (service instanceof DigitalRadioService digital)
                prof.broadcastPacket(
                        new Packet(new PlayerCommandPayload(new PlayerCommand(PlayerCommand.COMMAND_DIGITAL_TUNE,
                                Integer.toString(digital.getCurrentStation())))));
            else if (service instanceof AnalogRadioService analog) prof.broadcastPacket(
                    new Packet(new PlayerCommandPayload(new PlayerCommand(PlayerCommand.COMMAND_ANALOG_TUNE,
                            Integer.toString((int) (analog.getCurrentFrequency() / analog.getFrequencyStep()))))));

            if (service instanceof AdjustableGainService adjustable) prof.broadcastPacket(
                    new Packet(new PlayerCommandPayload(new PlayerCommand(PlayerCommand.COMMAND_ADJUST_GAIN,
                            Float.toString(adjustable.getCurrentGain())))));
        }

        return "Ok";
    }

    private RadioProfile getProfile(String profile) {
        RadioProfile prof = profiles.get(profile);
        if (prof == null) throw new ProfileNotFoundException(profile);
        return prof;
    }

    private static RadioService getCurrentService(RadioProfile prof) {
        int svc = prof.getCurrentService();
        if (svc < 0) throw new IllegalStateException("This profile has no started service.");
        return prof.getServices().get(svc);
    }
}
