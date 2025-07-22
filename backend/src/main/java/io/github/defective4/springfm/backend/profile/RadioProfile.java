package io.github.defective4.springfm.backend.profile;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.defective4.springfm.server.packet.Packet;
import io.github.defective4.springfm.server.packet.PacketGenerator;
import io.github.defective4.springfm.server.service.RadioService;

public class RadioProfile {
    private final List<DataOutputStream> connectedClients = new ArrayList<>();
    private int currentService = -1;
    private final List<RadioService> services;

    public RadioProfile(List<RadioService> services) {
        this.services = Collections.unmodifiableList(services);
        for (RadioService svc : services) svc.setPacketGenerator(new PacketGenerator() {
            List<DataOutputStream> toRemove = new ArrayList<>();

            @Override
            public void packetGenerated(Packet packet) {
                toRemove.clear();
                synchronized (connectedClients) {
                    for (DataOutputStream os : connectedClients) {
                        try {
                            packet.toStream(os);
                        } catch (Exception e) {
                            toRemove.add(os);
                        }
                    }
                }
                toRemove.forEach(RadioProfile.this::removeClient);
            }
        });
    }

    public void addClient(DataOutputStream os) throws IOException {
        synchronized (connectedClients) {
            connectedClients.add(os);
        }
        startCurrentService();
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
