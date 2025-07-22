package io.github.defective4.springfm.backend.profile;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
            List<OutputStream> toRemove = new ArrayList<>();

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
                synchronized (connectedClients) {
                    toRemove.forEach(connectedClients::remove);
                }
            }
        });
    }

    public void addClient(DataOutputStream os) {
        synchronized (connectedClients) {
            connectedClients.add(os);
        }
    }

    public void setActiveService(int index) throws IOException {
        if (index < -1) throw new IllegalArgumentException("Profile index can't be less than -1");
        if (index >= services.size()) throw new IllegalArgumentException("Profile index is too big");
        services.forEach(t -> {
            try {
                if (t.isStarted()) t.stop();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        if (index >= 0) {
            services.get(index).start();
        }
        currentService = index;
    }
}
