package io.github.defective4.springfm.server.service.impl;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.sound.sampled.AudioFormat;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import io.github.defective4.springfm.server.packet.DataGenerator;
import io.github.defective4.springfm.server.service.DigitalRadioService;
import io.github.defective4.springfm.server.service.ServiceArgument;
import io.github.defective4.springfm.server.util.RateLimiter;
import io.github.defective4.springfm.server.util.ScriptUtils;
import io.github.defective4.springfm.server.util.ThreadUtils;
import nl.digitalekabeltelevisie.data.mpeg.PSI;
import nl.digitalekabeltelevisie.data.mpeg.TransportStream;
import nl.digitalekabeltelevisie.data.mpeg.psi.PAT;
import nl.digitalekabeltelevisie.data.mpeg.psi.PATsection;
import nl.digitalekabeltelevisie.data.mpeg.psi.PATsection.Program;

public class DigitalTVService implements DigitalRadioService {

    public static class ServiceEntry {
        private final Map<String, Object> args;
        private final String source, sourceType;

        public ServiceEntry(String source, String sourceType, Map<String, Object> args) {
            this.source = source;
            this.sourceType = sourceType;
            this.args = args;
        }

        public Map<String, Object> getArgs() {
            return args == null ? Collections.emptyMap() : args;
        }

        public String getSource() {
            return source;
        }

        public String getSourceType() {
            return sourceType;
        }

        @Override
        public String toString() {
            return sourceType + (source == null ? "" : " - " + source);
        }
    }

    public static class TVProgram {
        private final int id;
        private final String name;
        private final ServiceEntry service;

        public TVProgram(int id, String name, ServiceEntry service) {
            this.id = id;
            this.name = name;
            this.service = service;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public ServiceEntry getService() {
            return service;
        }

        @Override
        public String toString() {
            return "TVProgram [id=" + id + ", name=" + name + "]";
        }

    }

    private boolean debug;
    private Process ffmpeg;
    private DataInputStream ffmpegInput;
    private OutputStream ffmpegOutput;
    private Future<?> ffmpegTask;
    private final AudioFormat format;
    private DataGenerator generator;
    private final List<Object> ignoredStations;
    private final String name;
    private final int probeTime;
    private final List<TVProgram> programs = new ArrayList<>();
    private final RateLimiter rateLimiter;
    private final List<ServiceEntry> services;
    private int station;

    private InputStream tsInput;
    private Process tsp;
    private Future<?> tsReceiverTask;

    public DigitalTVService(@ServiceArgument(defaultValue = "Digital TV", name = "name") String name,
            @ServiceArgument(name = "services") JsonArray serviceDefs,
            @ServiceArgument(name = "ignoredStations") JsonArray ignoredStations,
            @ServiceArgument(name = "probeTime", defaultValue = "10") Double probeTime, AudioFormat format) {
        this.name = name;
        this.probeTime = (int) (double) probeTime;
        this.format = format;
        Gson gson = new Gson();
        List<ServiceEntry> services = new ArrayList<>();
        for (JsonElement element : serviceDefs) {
            ServiceEntry entry = gson.fromJson(element, ServiceEntry.class);
            if (entry.getSourceType() == null)
                throw new IllegalArgumentException("One of defined services is missing a sourceType parameter");
            services.add(entry);
        }
        List<Object> ignoredStationsList = new ArrayList<>();
        for (JsonElement element : ignoredStations) if (element instanceof JsonPrimitive pr) if (pr.isNumber()) {
            ignoredStationsList.add(pr.getAsInt());
        } else {
            ignoredStationsList.add(pr.getAsString());
        }
        this.ignoredStations = Collections.unmodifiableList(ignoredStationsList);
        this.services = Collections.unmodifiableList(services);
        rateLimiter = new RateLimiter(
                (int) (format.getSampleRate() * (format.getSampleSizeInBits() / 8) * format.getChannels()));
    }

    @Override
    public Collection<String> checkMissingDependencies() {
        return Collections.emptyList();
    }

    @Override
    public AudioFormat getAudioFormat() {
        return format;
    }

    @Override
    public int getCurrentStation() {
        return station;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String[] getStations() {
        return programs.stream().map(prog -> prog.getName()).toList().toArray(new String[0]);
    }

    @Override
    public void init() throws IOException {
        programs.clear();
        for (ServiceEntry service : services) {
            System.err.println("Probing signal on \"" + service + "\"...");
            File tmpDir = Files.createTempDirectory("springfmtv").toFile();
            File tsFile = new File(tmpDir, "stream.ts");
            Process process = ScriptUtils.startProcess("tsp", tmpDir,
                    createTspArguments(service, "-P", "filter", "--psi-si", "-O", "file", tsFile.getPath()));
            try {
                process.waitFor(probeTime, TimeUnit.SECONDS);
            } catch (InterruptedException e) {}

            if (!tsFile.isFile()) throw new IOException("Couldn't probe signal on service \"" + service + "\"");

            System.err.println("Parsing PSI of \"" + service + "\"");
            try {
                TransportStream ts = new TransportStream(tsFile);
                ts.parseStream(null);
                PSI psi = ts.getPsi();
                PAT pat = psi.getPat();

                for (PATsection section : pat.getPATsections()) {
                    for (Program prog : section.getPrograms()) {
                        if (prog.getProgram_number() == 0 || ignoredStations.contains(prog.getProgram_number())
                                || ignoredStations.contains(prog.getServiceNameOrNit()))
                            continue;
                        programs.add(new TVProgram(prog.getProgram_number(), prog.getServiceNameOrNit(), service));
                    }
                }
            } catch (Exception e) {
                throw new IOException(e);
            }

            ScriptUtils.recursiveDelete(tmpDir);
        }

    }

    @Override
    public boolean isStarted() {
        return tsp != null && tsp.isAlive() && ffmpeg != null && ffmpeg.isAlive();
    }

    @Override
    public void setDebugMode(boolean debug) {
        this.debug = debug;
    }

    @Override
    public void setPacketGenerator(DataGenerator generator) {
        this.generator = generator;
    }

    @Override
    public void start() throws IOException {
        retuneTVStation();
    }

    @Override
    public void stop() throws IOException {
        try {
            if (tsp != null) tsp.destroyForcibly();
            if (ffmpeg != null) ffmpeg.destroyForcibly();

            if (tsReceiverTask != null) tsReceiverTask.cancel(true);
            if (ffmpegTask != null) ffmpegTask.cancel(true);

            if (tsInput != null) tsInput.close();
            if (ffmpegOutput != null) ffmpegOutput.close();
            if (ffmpegInput != null) ffmpegInput.close();
        } finally {
            tsp = null;
            ffmpeg = null;
        }
    }

    @Override
    public void tune(int stationIndex) throws IllegalArgumentException, IOException {
        station = stationIndex;
        retuneTVStation();
    }

    private void retuneTVStation() throws IOException {
        stop();
        TVProgram tvProgram = programs.get(station);
        ffmpeg = ScriptUtils.startProcess("ffmpeg", "-i", "-", "-ac", format.getChannels(), "-ar",
                (int) format.getSampleRate(), "-f", "wav", "-");
        tsp = ScriptUtils.startProcess("tsp", createTspArguments(tvProgram.getService(), "-P", "filter", "--service",
                tvProgram.getId(), "-O", "file", "-"));
        tsInput = tsp.getInputStream();
        ffmpegOutput = ffmpeg.getOutputStream();
        ffmpegInput = new DataInputStream(ffmpeg.getInputStream());

        tsReceiverTask = ThreadUtils.submit(() -> {
            try {
                byte[] buffer = new byte[1024];
                while (isStarted()) {
                    int read = tsInput.read(buffer);
                    if (read < 0) break;
                    ffmpegOutput.write(buffer, 0, read);
                }
            } catch (Exception e) {
                if (debug) e.printStackTrace();
                try {
                    stop();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });

        ffmpegTask = ThreadUtils.submit(() -> {
            try {
                byte[] buffer = new byte[4096];
                while (isStarted()) {
                    ffmpegInput.readFully(buffer);
                    rateLimiter.limit(buffer.length);
                    generator.audioSampleGenerated(buffer, false);
                }
            } catch (Exception e2) {
                if (debug) e2.printStackTrace();
                try {
                    stop();
                } catch (Exception e3) {
                    e3.printStackTrace();
                }
            }
        });
    }

    private static Object[] createTspArguments(ServiceEntry entry, Object... additional) {
        List<Object> args = new ArrayList<>();
        args.add("-I");
        args.add(entry.getSourceType().toLowerCase());
        if (entry.getSource() != null) args.add(entry.getSource());
        for (Entry<String, Object> e : entry.getArgs().entrySet()) {
            Object val = e.getValue();
            if (val instanceof String str) val = str.isBlank() ? null : "\"" + str + "\"";
            args.add("--" + e.getKey());
            if (val != null) args.add(String.valueOf(val));
        }
        Collections.addAll(args, additional);
        return args.toArray(new Object[0]);
    }

}
