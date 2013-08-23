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

package org.opentripplanner.updater.example;

import java.util.prefs.Preferences;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.GraphUpdater;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class shows an example of how to implement a graph updater. Besides implementing the methods
 * of the interface GraphUpdater, the updater also needs to be registered in the function
 * GraphUpdaterConfigurator.applyConfigurationToGraph.
 * 
 * While this example runs in a loop, it is better to use the abstract base class
 * PollingGraphUpdater for this purpose. The class ExamplePollingGraphUpdater shows an example of
 * this.
 * 
 * @see ExamplePollingGraphUpdater
 * @see GraphUpdaterConfigurator.applyConfigurationToGraph
 */
public class ExampleGraphUpdater implements GraphUpdater {

    private static Logger LOG = LoggerFactory.getLogger(ExampleGraphUpdater.class);

    private GraphUpdaterManager updaterManager;

    private Integer frequencySec;

    private String url;

    @Override
    public void run() {
        try {
            while (true) {
                // Sleep a given number of seconds
                Thread.sleep(frequencySec * 1000);
                LOG.info("Run example updater with hashcode: {}", this.hashCode());
                // Create example writer to "write to graph"
                ExampleGraphWriter exampleWriter = new ExampleGraphWriter();
                // Execute example writer
                updaterManager.execute(exampleWriter);
            }
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Configure updater for example usage.
     * 
     * Usage example ('example' name is an example):
     * 
     * <pre>
     * example.type = example-updater
     * example.frequencySec = 60
     * example.url = https://api.updater.com/example-updater
     * </pre>
     * 
     */
    @Override
    public void configure(Graph graph, Preferences preferences) throws Exception {
        frequencySec = preferences.getInt("frequencySec", 5);
        url = preferences.get("url", null);
        LOG.info("Configured example updater: frequencySec={} and url={}", frequencySec, url);
    }

    @Override
    public void setGraphUpdaterManager(GraphUpdaterManager updaterManager) {
        LOG.info("Example updater: updater manager is set");
        this.updaterManager = updaterManager;
    }

    @Override
    public void setup() {
        LOG.info("Setup example updater");
    }

    @Override
    public void teardown() {
        LOG.info("Teardown example updater");
    }

}
