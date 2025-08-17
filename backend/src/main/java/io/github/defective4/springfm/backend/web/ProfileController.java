package io.github.defective4.springfm.backend.web;

import static io.javalin.apibuilder.ApiBuilder.*;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import javax.sound.sampled.AudioFormat;

import com.google.gson.JsonObject;

import io.github.defective4.springfm.backend.config.ConfigurationReader;
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
import io.javalin.Javalin;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import io.javalin.validation.ValidationError;
import io.javalin.validation.ValidationException;

public class ProfileController {

    private final Javalin javalin;
    private final MessageDigest md;
    private final Map<String, RadioProfile> profiles;

    public ProfileController(ConfigurationReader config) throws NoSuchAlgorithmException {
        md = config.getMessageDigest();
        profiles = config.getAvailableProfiles();
        javalin = Javalin.create(cfg -> {
            cfg.jsonMapper(config.getGsonMapper());
            cfg.validation.register(RadioProfile.class, profiles::get);
            cfg.router.apiBuilder(() -> {
                path("/profile/{profile}", () -> {
                    post("/gain", ctx -> ctx.result(adjustGain(ctx)));
                    post("/service", ctx -> ctx.result(setService(ctx)));
                    path("/tune", () -> {
                        post("/digital", ctx -> ctx.result(digitalTune(ctx)));
                        post("/analog", ctx -> ctx.result(analogTune(ctx)));
                    });
                    get("/data", ctx -> dataStream(ctx));
                    get("/audio", ctx -> audioStream(ctx));
                });
            });
        });

        javalin.get("/auth", ctx -> ctx.json(auth()));
        javalin.get("/modules", ctx -> ctx.json(config.getModules().stream().map(mod -> mod.getModuleInfo()).toList()));

        javalin.exception(IllegalArgumentException.class, (ex, ctx) -> ctx.result(illegalArgument(ex)));
        javalin.exception(IllegalStateException.class, (ex, ctx) -> ctx.result(illegalArgument(ex)));
        javalin.exception(IOException.class, (ex, ctx) -> ctx.result(serverError(ex)));
        javalin.exception(ValidationException.class, (ex, ctx) -> ctx.result(validationException(ex)));
    }

    public String adjustGain(Context ctx) throws IllegalArgumentException, IOException {
        RadioProfile prof = getProfile(ctx);
        float gain = ctx.formParamAsClass("gain", Float.class).check(val -> val >= 0, "Gain can't be negative").get();

        RadioService service = getCurrentService(prof);
        if (service instanceof AdjustableGainService adjustable) {
            if (adjustable.getCurrentGain() == gain) return "Not changed";
            if (gain > adjustable.getMaxGain()) throw new IllegalArgumentException("Gain value out of bounds.");
            adjustable.setGain(gain);
            prof.broadcastPacket(new Packet(new PlayerCommandPayload(
                    new PlayerCommand(PlayerCommand.COMMAND_ADJUST_GAIN, Float.toString(gain)))));
            return "Ok";
        }
        throw new IllegalStateException("This service does not support gain adjusting");
    }

    public String analogTune(Context ctx) throws IllegalArgumentException, IOException {
        RadioProfile prof = getProfile(ctx);
        int frequency = ctx.formParamAsClass("frequency", Integer.class).get();
        RadioService service = getCurrentService(prof);
        if (service instanceof AnalogRadioService analog) {
            float absoluteFreq = frequency * analog.getFrequencyStep();
            if (absoluteFreq > analog.getMaxFrequency() || absoluteFreq < analog.getMinFrequency())
                throw new IllegalArgumentException("Frequency is out of range");
            if (analog.getCurrentFrequency() == absoluteFreq) return "Not changed";
            analog.tune(absoluteFreq);
            prof.broadcastPacket(new Packet(new PlayerCommandPayload(
                    new PlayerCommand(PlayerCommand.COMMAND_ANALOG_TUNE, Integer.toString(frequency)))));
            return "Ok";
        }
        throw new IllegalStateException("This service does not support analog tuning.");
    }

    public void audioStream(Context ctx) throws IOException {
        RadioProfile prof = getProfile(ctx);
        ctx.contentType(ContentType.AUDIO_WAV);
        OutputStream out = ctx.res().getOutputStream();
        int index = prof.getCurrentService();
        AudioFormat fmt;
        if (index < 0) {
            fmt = new AudioFormat(44.1e3f, 16, 1, true, false);
        } else {
            fmt = prof.getServices().get(index).getAudioFormat();
        }

        DataOutputStream dout = new DataOutputStream(out);
        dout.write(AudioUtils.createWavHeader(fmt));
        dout.flush();
        prof.addAudioClient(out);

        while (true) {
            try {
                Thread.sleep(1000);
                if (!prof.hasConnectedAudioClient(out)) {
                    return;
                }
            } catch (InterruptedException e) {}
        }
    }

    public AuthResponse auth() {
        return getAuthResponse();
    }

    public void dataStream(Context ctx) throws IOException {
        RadioProfile prof = getProfile(ctx);

        ctx.contentType(ContentType.APPLICATION_OCTET_STREAM);
        OutputStream out = ctx.res().getOutputStream();
        DataOutputStream os = new DataOutputStream(out);
        byte[] hash = getAuthResponse().hash(md);
        os.writeInt(hash.length);
        os.write(hash);
        prof.addDataClient(os);

        new Packet(new PlayerCommandPayload(
                new PlayerCommand(PlayerCommand.COMMAND_CHANGE_SERVICE, Integer.toString(prof.getCurrentService()))))
                .toStream(os);
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
                new Packet(new PlayerCommandPayload(new PlayerCommand(PlayerCommand.COMMAND_ADJUST_GAIN,
                        Float.toString(adjustable.getCurrentGain())))).toStream(os);
            }
        }
        os.flush();
        while (true) {
            try {
                Thread.sleep(1000);
                if (!prof.hasConnectedDataClient(os)) {
                    return;
                }
            } catch (InterruptedException e) {}
        }
    }

    public String digitalTune(Context ctx) throws IllegalArgumentException, IOException {
        RadioProfile prof = getProfile(ctx);
        int index = ctx.formParamAsClass("index", Integer.class).check(ix -> ix >= 0, "Station index can't be negative")
                .get();
        RadioService service = getCurrentService(prof);
        if (service instanceof DigitalRadioService digital) {
            if (digital.getCurrentStation() == index) return "Not changed";
            if (index >= digital.getStations().length)
                throw new IllegalArgumentException("Station index out of bounds.");
            digital.tune(index);
            prof.broadcastPacket(new Packet(new PlayerCommandPayload(
                    new PlayerCommand(PlayerCommand.COMMAND_DIGITAL_TUNE, Integer.toString(index)))));
            return "Ok";
        }
        throw new IllegalStateException("This service does not support digital tuning.");
    }

    public String illegalArgument(Exception e) {
        return getErrorJson(e.getMessage());
    }

    public String serverError(IOException e) {
        e.printStackTrace();
        return getErrorJson("There was an error on the server side.");
    }

    public String setService(Context ctx) throws IOException {
        RadioProfile prof = getProfile(ctx);
        int index = ctx.formParamAsClass("index", Integer.class).get();
        if (prof.getCurrentService() == index) {
            return "Not changed";
        }
        prof.setActiveService(index);
        prof.haltServices();
        prof.startCurrentService();
        AudioFormat fmt;
        if (index < 0) {
            fmt = new AudioFormat(44.1e3f, 16, 1, true, false);
        } else {
            fmt = prof.getServices().get(index).getAudioFormat();
        }
        prof.broadcastAudioSample(SerializableAudioFormat.Codec.createFormatSwitchFrame(fmt), false);
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

    public void start(int port) {
        javalin.start("0.0.0.0", port);
    }

    public String validationException(ValidationException ex) {
        JsonObject obj = new JsonObject();
        for (Entry<String, List<ValidationError<Object>>> entry : ex.getErrors().entrySet()) {
            obj.addProperty("field", entry.getKey());
            if (!entry.getValue().isEmpty()) obj.addProperty("error", entry.getValue().get(0).getMessage());
        }
        return obj.toString();
    }

    private AuthResponse getAuthResponse() {
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
                }).toList())).toList(), "A SpringFM instance", md.getAlgorithm());
    }

    private static RadioService getCurrentService(RadioProfile prof) {
        int svc = prof.getCurrentService();
        if (svc < 0) throw new IllegalStateException("This profile has no started service.");
        return prof.getServices().get(svc);
    }

    private static String getErrorJson(String message) {
        JsonObject errorObject = new JsonObject();
        errorObject.addProperty("error", message);
        return errorObject.toString();
    }

    private static RadioProfile getProfile(Context ctx) {
        return ctx.pathParamAsClass("profile", RadioProfile.class).getOrThrow(map -> {
            return new IllegalArgumentException("Profile \"" + ctx.pathParam("profile") + "\" was not found.");
        });
    }
}
