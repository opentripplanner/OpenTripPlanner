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

package org.opentripplanner.updater;

/**
 * Interface for graph updaters. Objects that implement this interface should always be configured
 * via PreferencesConfigurable.configure after creating the object. GraphUpdaterConfigurator should
 * take care of that. Beware that updaters run in separate threads at the same time.
 * 
 * The only allowed way to make changes to the graph in an updater is by executing (anonymous)
 * GraphWriterRunnable objects via GraphUpdaterManager.execute.
 * 
 * Example implementations can be found in ExampleGraphUpdater and ExamplePollingGraphUpdater.
 * 
 * @see ExampleGraphUpdater
 * @see ExamplePollingGraphUpdater
 * @see GraphUpdaterManager.execute
 * @see GraphUpdaterConfigurator
 */
public interface GraphUpdater extends JsonConfigurable {

    /**
     * Graph updaters must be aware of their manager to be able to execute GraphWriterRunnables.
     * GraphUpdaterConfigurator should take care of calling this function.
     * 
     * @param updaterManager is the parent updater manager
     * @see GraphWriterRunnable
     * @see GraphUpdaterManager.execute
     */
    public void setGraphUpdaterManager(GraphUpdaterManager updaterManager);

    /**
     * Here the updater can be initialized. If it throws, the updater won't be started (i.e. the run
     * method won't be called).
     */
    public void setup() throws Exception;

    /**
     * This is where the updater thread receives updates and applies them to the graph. This method
     * only runs once.
     */
    public void run() throws Exception;

    /**
     * Here the updater can cleanup after itself.
     */
    public void teardown();

}
