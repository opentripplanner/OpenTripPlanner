package org.opentripplanner.narrative;

import java.util.List;
import java.util.Vector;

import org.opentripplanner.jags.core.TransportationMode;
import org.opentripplanner.jags.edgetype.Traversable;
import org.opentripplanner.jags.spt.GraphPath;
import org.opentripplanner.jags.spt.SPTEdge;
import org.opentripplanner.jags.spt.SPTVertex;

import com.vividsolutions.jts.geom.Geometry;

class BasicNarrativeItem implements NarrativeItem {
    private String name;

    private String direction;

    private Geometry geometry;

    private String start;

    private String end;

    private double distance;

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getDirection() {
        return direction;
    }

    public String getTowards() {
        return null;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public Geometry getGeometry() {
        return geometry;
    }

    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }

    public void addGeometry(Geometry geometry) {
        this.geometry = this.geometry.union(geometry);
    }

    public String getStart() {
        return start;
    }

    public String getEnd() {
        return end;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public double getDistanceKm() {
        return distance;
    }
}

public class Narrative {
    protected GraphPath path;

    protected Vector<NarrativeSection> sections = null;

    public Vector<NarrativeSection> getSections() {
        return sections;
    }

    public Narrative(GraphPath path) {
        this.path = path;
        if (path.edges.size() == 0) {
            return;
        }
        this.sections = new Vector<NarrativeSection>();

        int i = 0;
        String lastName = path.edges.elementAt(i).payload.getName();
        TransportationMode lastMode = null;
        Vector<SPTEdge> currentSection = new Vector<SPTEdge>();
        int startVertex = 0;
        for (SPTEdge edge : path.edges) {
            Traversable traversable = edge.payload;
            String edgeName = traversable.getName();
            if (!edgeName.equals(lastName)
                    && !(traversable.getMode() == TransportationMode.WALK && lastMode == TransportationMode.WALK)) {
                // A section ends when the name of the payload changes except when walking
                List<SPTVertex> currentVertices = path.vertices.subList(startVertex, i + 1);
                // Don't add boarding and alighting edges as separate sections in the narrative
                if (lastMode != TransportationMode.ALIGHTING
                        && lastMode != TransportationMode.BOARDING)
                    sections.add(new NarrativeSection(currentVertices, currentSection));
                currentSection.clear();
                lastName = edgeName;
                startVertex = i;
            }
            i += 1;
            lastMode = traversable.getMode();
            currentSection.add(edge);
        }
        // Add the last section, unless it's an alight
        if (lastMode != TransportationMode.ALIGHTING) {
            sections.add(new NarrativeSection(path.vertices.subList(startVertex, i + 1),
                    currentSection));
        }
    }

    public String asText() {
        if (sections == null)
            return "No path";
        String out = "";
        for (NarrativeSection section : sections) {
            out += section.asText() + "\n";
        }
        return out;
    }

}
