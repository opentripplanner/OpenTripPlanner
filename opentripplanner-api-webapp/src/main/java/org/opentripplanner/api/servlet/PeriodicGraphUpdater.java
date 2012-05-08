/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.api.servlet;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class PeriodicGraphUpdater {
    private UpdateTask updater;

    private long updateFrequency = 1000 * 60 * 5; // five minutes

    private Thread thread;

    private List<Runnable> updaters = Collections.emptyList();

    public void start() {
        updater = new UpdateTask();
        thread = new Thread(updater);
        thread.setDaemon(false);
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

    public List<Runnable> getUpdaters() {
        return updaters;
    }

    public void setUpdaters(List<Runnable> updaters) {
        this.updaters = updaters;
    }

    class UpdateTask implements Runnable {
        boolean finished = false;

        public void finish() {
            this.finished = true;
            Thread.currentThread().interrupt();
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
                for (Runnable updater : getUpdaters()) {
                    System.out.println("running updater " + updater);
                    updater.run();
                }

                lastUpdate = now;

                Thread.yield();
            }
        }

    }

}
