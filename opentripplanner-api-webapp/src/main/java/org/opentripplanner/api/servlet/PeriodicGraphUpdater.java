package org.opentripplanner.api.servlet;

import java.util.List;

import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.updater.Updater;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PeriodicGraphUpdater {
    GraphService graphService;

    private UpdateTask updater;

    private long updateFrequency = 1000 * 60 * 5; // five minutes

    private Thread thread;

    private List<Updater> updaters;

    @Autowired
    public void setGraphService(GraphService graphService) {
        this.graphService = graphService;
    }

    public void start() {
        updater = new UpdateTask();
        thread = new Thread(updater);
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        updater.finish();
        thread.interrupt();
    }

    public void setUpdateFrequency(long updateFrequency) {
        this.updateFrequency = updateFrequency;
    }

    public long getUpdateFrequency() {
        return updateFrequency;
    }

    public List<Updater> getUpdaters() {
        return updaters;
    }

    public void setUpdaters(List<Updater> updaters) {
        this.updaters = updaters;
    }

    class UpdateTask implements Runnable {
        boolean finished = false;

        public void finish() {
            this.finished = true;
        }

        @Override
        public void run() {
            long lastUpdate = 0;
            while (!finished) {
                long now = System.currentTimeMillis();
                while (now - lastUpdate < getUpdateFrequency()) {
                    try {
                        Thread.sleep(getUpdateFrequency() - (now - lastUpdate));
                    } catch (InterruptedException e) {
                        if (finished) {
                            return;
                        }
                    }

                    now = System.currentTimeMillis();
                }
                for (Updater updater : getUpdaters()) {
                    System.out.println("running updater " + updater.getUrl());
                    updater.setGraphService(graphService);
                    updater.run();
                }

                lastUpdate = now;

                Thread.yield();
            }
        }

    }

}
