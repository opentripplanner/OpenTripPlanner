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
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.PollingGraphUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class shows an example of how to implement a polling graph updater. Besides implementing the
 * methods of the interface PollingGraphUpdater, the updater also needs to be registered in the
 * function GraphUpdaterConfigurator.applyConfigurationToGraph.
 * 
 * This example is suited for polling updaters. For streaming updaters (aka push updaters) it is
 * better to use GraphUpdater interface directly for this purpose. The class ExampleGraphUpdater
 * shows an example of how to implement this.
 * 
 * @see ExampleGraphUpdater
 * @see GraphUpdaterConfigurator.applyConfigurationToGraph
 */
public class ExamplePollingGraphUpdater extends PollingGraphUpdater {

    private static Logger LOG = LoggerFactory.getLogger(ExamplePollingGraphUpdater.class);

    private GraphUpdaterManager updaterManager;

    private String url;

    @Override
    protected void runPolling() {
        LOG.info("Run example polling updater with hashcode: {}", this.hashCode());
        // Create example writer to "write to graph"
        ExampleGraphWriter exampleWriter = new ExampleGraphWriter();
        // Execute example writer
        updaterManager.execute(exampleWriter);
    }

    /**
     * Configure polling updater for example usage.
     * 
     * Usage example ('polling-example' name is an example):
     * 
     * <pre>
     * polling-example.type = example-polling-updater
     * polling-example.frequencySec = 60
     * polling-example.url = https://api.updater.com/example-polling-updater
     * </pre>
     * 
     * The property frequencySec is read and used by the abstract base class. 
     */
    @Override
    protected void configurePolling(Graph graph, Preferences preferences) throws Exception {
        url = preferences.get("url", null);
        LOG.info("Configured example polling updater: frequencySec={} and url={}",
                getFrequencySec(), url);
    }

    @Override
    public void setGraphUpdaterManager(GraphUpdaterManager updaterManager) {
        LOG.info("Example polling updater: updater manager is set");
        this.updaterManager = updaterManager;
    }

    @Override
    public void setup() {
        LOG.info("Setup example polling updater");
    }

    @Override
    public void teardown() {
        LOG.info("Teardown example polling updater");
    }

}
