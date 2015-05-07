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

package org.opentripplanner.graph_builder.module.bike;

import java.util.Collection;
import java.util.HashMap;

import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.routing.bike_park.BikePark;
import org.opentripplanner.routing.bike_rental.BikeRentalStationService;
import org.opentripplanner.routing.edgetype.BikeParkEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.BikeParkVertex;
import org.opentripplanner.updater.bike_park.BikeParkDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This graph builder allow one to statically build bike park using the same source as the dynamic
 * bike park updater.
 * 
 * Bike park-and-ride and "OV-fiets mode" development has been funded by GoAbout
 * (https://goabout.com/).
 * 
 * @author laurent
 * @author GoAbout
 */
public class BikeParkModule implements GraphBuilderModule {

    private static Logger LOG = LoggerFactory.getLogger(BikeParkModule.class);

    private BikeParkDataSource dataSource;

    public void setDataSource(BikeParkDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {

        LOG.info("Building bike parks from static source...");
        BikeRentalStationService service = graph.getService(BikeRentalStationService.class, true);
        if (!dataSource.update()) {
            LOG.warn("No bike parks found from the data source.");
            return;
        }
        Collection<BikePark> bikeParks = dataSource.getBikeParks();

        for (BikePark bikePark : bikeParks) {
            service.addBikePark(bikePark);
            BikeParkVertex bikeParkVertex = new BikeParkVertex(graph, bikePark);
            new BikeParkEdge(bikeParkVertex);
        }
        LOG.info("Created " + bikeParks.size() + " bike parks.");
    }

    @Override
    public void checkInputs() {
    }
}
