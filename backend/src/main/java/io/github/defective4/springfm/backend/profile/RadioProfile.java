package io.github.defective4.springfm.backend.profile;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import io.github.defective4.springfm.server.packet.DataGenerator;
import io.github.defective4.springfm.server.packet.Packet;
import io.github.defective4.springfm.server.packet.impl.KeepAlivePayload;
import io.github.defective4.springfm.server.service.RadioService;

public class RadioProfile {

    private static final Timer KEEP_ALIVE_TIMER = new Timer(true);

    private final List<OutputStream> clientsToRemove = new ArrayList<>();
    private final List<OutputStream> connectedAudioClients = new ArrayList<>();
    private final List<DataOutputStream> connectedDataClients = new ArrayList<>();

    private int currentService = -1;
    private final List<RadioService> services;

    public RadioProfile(List<RadioService> services) {
        this.services = Collections.unmodifiableList(services);
        for (RadioService svc : services) svc.setPacketGenerator(new DataGenerator() {

            @Override
            public void audioSampleGenerated(byte[] buffer, boolean bigEndian) {
                broadcastAudioSample(buffer, bigEndian);
            }

            @Override
            public void packetGenerated(Packet packet) {
                broadcastPacket(packet);
            }
        });
        KEEP_ALIVE_TIMER.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                broadcastPacket(new Packet(new KeepAlivePayload(System.currentTimeMillis())));
            }
        }, 0, 10000L);
    }

    public void addAudioClient(OutputStream os) throws IOException {
        synchronized (connectedAudioClients) {
            connectedAudioClients.add(os);
        }
        startCurrentService();
    }

    public void addDataClient(DataOutputStream os) throws IOException {
        synchronized (connectedDataClients) {
            connectedDataClients.add(os);
        }
        startCurrentService();
    }

    public synchronized void broadcastAudioSample(byte[] buffer, boolean swapEndianness) {
        if (buffer.length != 4096) throw new IllegalArgumentException("Audio frame must be 4096 bytes long");
        if (swapEndianness) for (int i = 0; i < buffer.length; i += 2) {
            byte b = buffer[i];
            buffer[i] = buffer[i + 1];
            buffer[i + 1] = b;
        }
        clientsToRemove.clear();
        synchronized (connectedAudioClients) {
            for (OutputStream os : connectedAudioClients) {
                try {
                    os.write(buffer);
                    os.flush();
                } catch (Exception e) {
                    clientsToRemove.add(os);
                }
            }
        }
        clientsToRemove.forEach(RadioProfile.this::removeClient);
    }

    public synchronized void broadcastPacket(Packet packet) {
        clientsToRemove.clear();
        synchronized (connectedDataClients) {
            for (DataOutputStream os : connectedDataClients) {
                try {
                    packet.toStream(os);
                    os.flush();
                } catch (Exception e) {
                    clientsToRemove.add(os);
                }
            }
        }
        clientsToRemove.forEach(RadioProfile.this::removeClient);
    }

    public int getCurrentService() {
        return currentService;
    }

    public List<RadioService> getServices() {
        return Collections.unmodifiableList(services);
    }

    public void haltServices() {
        synchronized (services) {
            services.forEach(t -> {
                try {
                    if (t.isStarted()) t.stop();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public boolean hasConnectedAudioClient(OutputStream dos) {
        synchronized (connectedAudioClients) {
            return connectedAudioClients.contains(Objects.requireNonNull(dos));
        }
    }

    public boolean hasConnectedDataClient(DataOutputStream dos) {
        synchronized (connectedDataClients) {
            return connectedDataClients.contains(Objects.requireNonNull(dos));
        }
    }

    public void setActiveService(int index) {
        if (index < -1) throw new IllegalArgumentException("Profile index can't be less than -1");
        if (index >= services.size()) throw new IllegalArgumentException("Profile index is too big");
        currentService = index;
    }

    public void startCurrentService() throws IOException {
        synchronized (services) {
            if (currentService >= 0) {
                RadioService service = services.get(currentService);
                if (!service.isStarted()) service.start();
            }
        }
    }

    private void removeClient(OutputStream os) {
        synchronized (connectedDataClients) {
            connectedDataClients.remove(os);
        }
        synchronized (connectedAudioClients) {
            connectedAudioClients.remove(os);
        }
        if (connectedDataClients.isEmpty() && connectedAudioClients.isEmpty()) haltServices();
    }
}
