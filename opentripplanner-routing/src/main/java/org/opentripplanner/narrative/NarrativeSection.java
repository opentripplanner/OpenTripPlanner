package org.opentripplanner.narrative;

import java.util.List;
import java.util.Vector;

import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.TransportationMode;
import org.opentripplanner.routing.edgetype.Hop;
import org.opentripplanner.routing.edgetype.Street;
import org.opentripplanner.routing.spt.SPTEdge;
import org.opentripplanner.routing.spt.SPTVertex;

import com.vividsolutions.jts.geom.Geometry;

/**
 * A narrative section represents the portion of a trip which takes place on a single conveyance.
 * For example, a trip from Avenue H on the Q to Prince St on the R transferring at Canal St would
 * be represented as a NarrativeSection from Avenue H to Canal and another from Canal to Prince.
 * Normally, there would also be a NarrativeSection for the walk to the Avenue H station and from
 * the Prince St station to the final destination.
 */
public class NarrativeSection {
    public Vector<NarrativeItem> items;

    TransportationMode mode;

    String name;

    String direction;

    Geometry geometry;

    private long startTime;

    private long endTime;

    public Geometry getGeometry() {
        return geometry;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public Vector<NarrativeItem> getItems() {
        return items;
    }

    public TransportationMode getMode() {
        return mode;
    }

    public String getName() {
        return name;
    }

    public String getDirection() {
        return direction;
    }

    public NarrativeSection(List<SPTVertex> vertices, Vector<SPTEdge> edges) {
        /* compute start and end times */
        SPTVertex first = vertices.get(0);
        startTime = first.state.getTime();
        SPTVertex last = vertices.get(vertices.size() - 1);
        endTime = last.state.getTime();

        items = new Vector<NarrativeItem>();
        Edge graphEdge = edges.firstElement().payload;
        mode = graphEdge.getMode();
        geometry = graphEdge.getGeometry();
        for (SPTEdge edge : edges.subList(1, edges.size())) {
            geometry = geometry.union(edge.payload.getGeometry());
        }

        if (graphEdge instanceof Hop) {
            name = graphEdge.getName();
            direction = graphEdge.getDirection();
            BasicNarrativeItem item = new BasicNarrativeItem();
            item.setDirection(graphEdge.getDirection());
            item.setName(graphEdge.getName());
            String end = graphEdge.getEnd();
            Geometry geom = graphEdge.getGeometry();

            double totalDistance = graphEdge.getDistance();
            for (SPTEdge edge : edges.subList(1, edges.size())) {
                graphEdge = edge.payload;
                totalDistance += graphEdge.getDistance();
                geom = geom.union(graphEdge.getGeometry());
                end = graphEdge.getEnd();
            }
            item.setGeometry(geom);
            item.setDistance(totalDistance);
            item.setEnd(end);
            items.add(item);
        } else if (graphEdge instanceof Street) {
            name = "walk";
            Street street1 = (Street) edges.firstElement().payload;
            Street street2 = (Street) edges.lastElement().payload;
            direction = Street.computeDirection(street1.getGeometry().getStartPoint(), street2
                    .getGeometry().getEndPoint());

            double totalDistance = 0;
            String lastStreet = null;
            BasicNarrativeItem item = null;
            for (SPTEdge edge : edges) {
                graphEdge = edge.payload;
                String streetName = graphEdge.getName();
                if (streetName == lastStreet) {
                    totalDistance += graphEdge.getDistance();
                    item.setDistance(totalDistance);
                    item.setGeometry(item.getGeometry().union(graphEdge.getGeometry()));
                    item.setEnd(graphEdge.getStart());
                    continue;
                }
                item = new BasicNarrativeItem();
                item.setName(streetName);
                item.setDirection(graphEdge.getDirection());
                item.setGeometry(graphEdge.getGeometry());
                item.setStart(graphEdge.getStart());
                item.setEnd(graphEdge.getEnd());
                items.add(item);
            }
        }
    }

    public String toString() {
        if (mode == TransportationMode.TRANSFER) {
            return "transfer";
        }
        return direction + " on " + name + " via " + mode + "(" + items.size() + " items)";
    }
}
