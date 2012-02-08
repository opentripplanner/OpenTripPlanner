package org.opentripplanner.graph_builder.services.osm;

import org.opentripplanner.graph_builder.model.osm.OSMWay;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.graph.Graph;

/**
 * For when CreativeNamePicker/WayPropertySet is just not powerful enough.
 * 
 * @author novalis
 * 
 */
public interface CustomNamer {
    public String name(OSMWay way, String defaultName);

    public void nameWithEdge(OSMWay way, PlainStreetEdge edge);

    public void postprocess(Graph graph);
}
