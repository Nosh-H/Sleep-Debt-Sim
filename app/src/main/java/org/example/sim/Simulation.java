package org.example.sim;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Simulation {
    public interface Listener {
        void onTick(long step, double value);
    }

    private final List<Listener> listeners = new ArrayList<>();
    private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
    private long step = 0;
    private double value = 0.0;
    private boolean running = false;

    public void addListener(Listener l) {
        synchronized (listeners) {
            listeners.add(l);
        }
    }

    public void removeListener(Listener l) {
        synchronized (listeners) {
            listeners.remove(l);
        }
    }

    public void start(long periodMillis) {
        if (running) return;
        running = true;
        exec.scheduleAtFixedRate(() -> {
            step++;
            // Simple demo: a sine-wave like value
            value = Math.sin(step * 0.1);
            notifyListeners();
        }, 0, periodMillis, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        running = false;
        exec.shutdownNow();
    }

    private void notifyListeners() {
        synchronized (listeners) {
            for (Listener l : listeners) {
                try {
                    l.onTick(step, value);
                } catch (Exception ignored) {
                }
            }
        }
    }
}
