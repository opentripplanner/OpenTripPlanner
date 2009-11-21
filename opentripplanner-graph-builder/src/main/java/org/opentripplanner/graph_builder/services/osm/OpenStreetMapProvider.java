package org.opentripplanner.graph_builder.services.osm;


public interface OpenStreetMapProvider {
    public void readOSM(OpenStreetMapContentHandler handler);
}
