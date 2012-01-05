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

package org.opentripplanner.routing.patch;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.services.TransitIndexService;
import org.opentripplanner.routing.transit_index.RouteSegment;
import org.opentripplanner.routing.transit_index.RouteVariant;

/**
 * This adds a note to all boardings of a given route or stop (optionally, in a given direction)
 * 
 * @author novalis
 * 
 */
@XmlRootElement(name = "AlertPatch")
public class AlertPatch extends AbstractPatch {
    private static final long serialVersionUID = -7947169269916558755L;

    private AgencyAndId route;

    private AgencyAndId stop;

    private String direction;

    public AlertPatch() {
    }

    @Override
    public void remove(Graph graph) {
        if (route != null) {
            TransitIndexService index = graph.getService(TransitIndexService.class);
            List<RouteVariant> variants = index.getVariantsForRoute(route);
            for (RouteVariant variant : variants) {
                if (direction != null && !direction.equals(variant.getDirection())) {
                    continue;
                }
                for (RouteSegment segment : variant.getSegments()) {
                    if (stop == null || segment.stop.equals(stop)) {
                        if (segment.board != null) {
                            segment.board.removePatch(this);
                        }
                    }
                }
            }
        } else if (stop != null) {
            TransitIndexService index = graph.getService(TransitIndexService.class);
            Edge edge = index.getPreboardEdge(stop);
            edge.removePatch(this);
            edge = index.getPrealightEdge(stop);
            edge.removePatch(this);
        }
    }

    @Override
    public void apply(Graph graph) {

        if (route != null) {
            TransitIndexService index = graph.getService(TransitIndexService.class);
            List<RouteVariant> variants = index.getVariantsForRoute(route);
            for (RouteVariant variant : variants) {
                if (direction != null && !direction.equals(variant.getDirection())) {
                    continue;
                }
                for (RouteSegment segment : variant.getSegments()) {
                    if (stop == null || segment.stop.equals(stop)) {
                        if (segment.board != null) {
                            segment.board.addPatch(this);
                        }
                    }
                }
            }
        } else if (stop != null) {
            TransitIndexService index = graph.getService(TransitIndexService.class);
            Edge edge = index.getPreboardEdge(stop);
            edge.addPatch(this);
            edge = index.getPrealightEdge(stop);
            edge.addPatch(this);
        }
    }

    @Override
    public void filterTraverseResult(StateEditor result) {
        result.addAlert(alert);
    }

    @XmlJavaTypeAdapter(AgencyAndIdAdapter.class)
    public AgencyAndId getRoute() {
        return route;
    }

    @XmlJavaTypeAdapter(AgencyAndIdAdapter.class)
    public AgencyAndId getStop() {
        return stop;
    }

    public void setRoute(AgencyAndId route) {
        this.route = route;
    }

    public void setDirection(String direction) {
        if (direction != null && direction.equals("")) {
            direction = null;
        }
        this.direction = direction;
    }

    @XmlElement
    public String getDirection() {
        return direction;
    }

    public void setStop(AgencyAndId stop) {
        this.stop = stop;
    }

    public boolean equals(Object o) {
        if (!(o instanceof AlertPatch)) {
            return false;
        }
        AlertPatch other = (AlertPatch) o;
        if (direction == null) {
            if (other.direction != null) {
                return false;
            }
        } else {
            if (!direction.equals(other.direction)) {
                return false;
            }
        }
        if (stop == null) {
            if (other.stop != null) {
                return false;
            }
        } else {
            if (!stop.equals(other.stop)) {
                return false;
            }
        }
        if (route == null) {
            if (other.route != null) {
                return false;
            }
        } else {
            if (!route.equals(other.route)) {
                return false;
            }
        }
        return other.alert.equals(alert) && super.equals(other);
    }
}
