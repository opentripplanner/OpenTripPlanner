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

package org.opentripplanner.graph_builder.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.opentripplanner.graph_builder.services.GraphBuilder;
import org.opentripplanner.routing.services.ChainedFareService;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.FareService;

/**
 * Builds a street graph from OpenStreetMap data.
 * 
 */
public class FareServiceGraphBuilderImpl implements GraphBuilder {

    private ChainedFareService service;

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
        FareService existingService = graph.getService(FareService.class);
        service.setNextService(existingService);
        graph.putService(FareService.class, service);
    }

    public void setService(ChainedFareService service) {
        this.service = service;
    }

    @Override
    public List<String> provides() {
        return Arrays.asList("fares");
    }

    @Override
    public List<String> getPrerequisites() {
        return Collections.emptyList();
    }

    @Override
    public void checkInputs() {
        // nothing to do
    }
}