package org.opentripplanner.narrative.model;

import java.util.List;
import java.util.Vector;

import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.TransportationMode;
import org.opentripplanner.routing.edgetype.HoppableEdge;
import org.opentripplanner.routing.edgetype.Street;
import org.opentripplanner.routing.edgetype.WalkableEdge;
import org.opentripplanner.routing.spt.SPTEdge;
import org.opentripplanner.routing.spt.SPTVertex;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

/**
 * A narrative section represents the portion of a trip which takes place on a single conveyance.
 * For example, a trip from Avenue H on the Q to Prince St on the R transferring at Canal St would
 * be represented as a NarrativeSection from Avenue H to Canal and another from Canal to Prince.
 * Normally, there would also be a NarrativeSection for the walk to the Avenue H station and from
 * the Prince St station to the final destination.
 */
public class NarrativeSection {
    
    private static GeometryFactory _geometryFactory = new GeometryFactory();
    
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
            geometry = joinGeometries(geometry,edge.payload.getGeometry());
        }

        if (graphEdge instanceof HoppableEdge) {
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
                geom = joinGeometries(geom,graphEdge.getGeometry());
                end = graphEdge.getEnd();
            }
            item.setGeometry(geom);
            item.setDistance(totalDistance);
            item.setEnd(end);
            items.add(item);
        } else if (graphEdge instanceof WalkableEdge) {
            name = "walk";
            WalkableEdge street1 = (WalkableEdge) edges.firstElement().payload;
            WalkableEdge street2 = (WalkableEdge) edges.lastElement().payload;
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


    public String asText() {
        if (mode == TransportationMode.TRANSFER) {
            return "transfer";
        }
        return direction + " on " + name + " via " + mode + "(" + items.size() + " items)";
    }
    
    private Geometry joinGeometries(Geometry g1, Geometry g2){
        
        // TODO - Maybe there is a better way of doing this?
        if( (g1 instanceof LineString) && (g2 instanceof LineString) ) {
            LineString ls1 = (LineString) g1;
            LineString ls2 = (LineString) g2;
            
            Coordinate[] from = ls1.getCoordinates();
            Coordinate[] to = ls2.getCoordinates();
            
            if( from.length == 0)
                return g2;
            
            if( to.length == 0)
                return g1;
            
            Coordinate prevPoint = from[from.length-1];
            Coordinate nextPoint = to[0];
            
            if( prevPoint.x == nextPoint.x && prevPoint.y == nextPoint.y ) {
                Coordinate[] joint = new Coordinate[from.length + to.length -1];
                System.arraycopy(from, 0, joint, 0, from.length);
                System.arraycopy(to, 0, joint, from.length - 1, to.length);
                return _geometryFactory.createLineString(joint);
            }
        }
        if (g1 == null) {
            return g2;
        } else if (g2 == null) {
            return g1;
        }
        return g1.union(g2);
    }

}
