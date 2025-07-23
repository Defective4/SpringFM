package io.github.defective4.springfm.backend.profile;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import io.github.defective4.springfm.server.packet.Packet;
import io.github.defective4.springfm.server.packet.PacketGenerator;
import io.github.defective4.springfm.server.packet.impl.KeepAlivePayload;
import io.github.defective4.springfm.server.service.RadioService;

public class RadioProfile {

    private static final Timer KEEP_ALIVE_TIMER = new Timer(true);

    private final List<DataOutputStream> clientsToRemove = new ArrayList<>();
    private final List<DataOutputStream> connectedClients = new ArrayList<>();
    private int currentService = -1;

    private final List<RadioService> services;

    public RadioProfile(List<RadioService> services) {
        this.services = Collections.unmodifiableList(services);
        for (RadioService svc : services) svc.setPacketGenerator(new PacketGenerator() {

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

    public void addClient(DataOutputStream os) throws IOException {
        synchronized (connectedClients) {
            connectedClients.add(os);
        }
        startCurrentService();
    }

    public synchronized void broadcastPacket(Packet packet) {
        clientsToRemove.clear();
        synchronized (connectedClients) {
            for (DataOutputStream os : connectedClients) {
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

    public void removeClient(DataOutputStream os) {
        synchronized (connectedClients) {
            connectedClients.remove(os);
        }
        if (connectedClients.isEmpty()) haltServices();
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
}
