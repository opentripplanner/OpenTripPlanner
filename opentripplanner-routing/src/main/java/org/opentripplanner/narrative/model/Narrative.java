package org.opentripplanner.narrative.model;

import java.util.List;
import java.util.Vector;

import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.TransportationMode;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.SPTEdge;
import org.opentripplanner.routing.spt.SPTVertex;

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
        for (SPTEdge sptEdge : path.edges) {
            Edge edge = sptEdge.payload;
            String edgeName = edge.getName();
            if (!edgeName.equals(lastName)
                    && !(edge.getMode() == TransportationMode.WALK && lastMode == TransportationMode.WALK)) {
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
            lastMode = edge.getMode();
            currentSection.add(sptEdge);
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
