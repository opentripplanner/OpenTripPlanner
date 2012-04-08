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

package org.opentripplanner.api.ws.analysis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlRootElement;

import org.opentripplanner.analysis.AnalysisUtils;
import org.opentripplanner.api.model.analysis.GraphComponent;
import org.opentripplanner.api.model.analysis.GraphComponentPolygons;
import org.opentripplanner.api.ws.SearchResource;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.core.RouteSpec;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.util.DateUtils;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.security.access.annotation.Secured;

import com.sun.jersey.api.spring.Autowire;
import com.sun.jersey.spi.resource.Singleton;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Data about the components (in a graph-theoretical sense) of the graph. This is useful for
 * debugging connectivity and routing problems.
 *
 * @author novalis
 *
 */
@Path("/components")
@XmlRootElement
@Autowire
@Singleton
public class Components {
    private GraphService graphService;

    /**
     * cache for component geometry (for a specific query)
     */
    private List<Geometry> cachedPolygons;
    private TraverseOptions cachedOptions;
    private long cachedDateTime;

    @Required
    public void setGraphService(GraphService graphService) {
        this.graphService = graphService;
    }

    /**
     * Get polygons covering the components of the graph. The largest component (in terms of number
     * of nodes) will not overlap any other components (it will have holes); the others may overlap
     * each other.
     *
     * @param modes
     * @return
     */
    @Secured({ "ROLE_USER" })
    @GET
    @Path("/polygons")
    @Produces({ MediaType.APPLICATION_JSON })
    public GraphComponentPolygons getComponentPolygons(
            @DefaultValue("TRANSIT,WALK") @QueryParam("modes") TraverseModeSet modes,
            @QueryParam(SearchResource.DATE) String date, @QueryParam(SearchResource.TIME) String time,
            @DefaultValue("") @QueryParam(SearchResource.BANNED_ROUTES) String bannedRoutes) {

        TraverseOptions options = new TraverseOptions(modes);
        options.bannedRoutes = new HashSet<RouteSpec>();
        if (bannedRoutes.length() > 0) {
            for (String element : bannedRoutes.split(",")) {
                String[] routeSpec = element.split("_", 2);
                if (routeSpec.length != 2) {
                    throw new IllegalArgumentException(
                            "AgencyId or routeId not set in bannedRoutes list");
                }
                options.bannedRoutes.add(new RouteSpec(routeSpec[0], routeSpec[1]));
            }
        }

        long dateTime = DateUtils.toDate(date, time).getTime();
        if (cachedPolygons == null || dateTime != cachedDateTime || !options.equals(cachedOptions)) {
            cachedOptions = options;
            cachedDateTime = dateTime;
            Graph graph = graphService.getGraph();
            if (graphService.getCalendarService() != null) {
                options.setCalendarService(graphService.getCalendarService());
            }
            options.setServiceDays(dateTime, graph.getAgencyIds());
            cachedPolygons = AnalysisUtils.getComponentPolygons(graph, options, dateTime);
        }
        
        GraphComponentPolygons out = new GraphComponentPolygons();
        out.components = new ArrayList<GraphComponent>();

        for (Geometry geometry : cachedPolygons) {
            GraphComponent component = new GraphComponent();
            component.polygon = geometry;
            out.components.add(component);
        }

        return out;
    }
}
