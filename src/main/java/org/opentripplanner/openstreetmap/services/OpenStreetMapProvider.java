package org.opentripplanner.openstreetmap.services;


public interface OpenStreetMapProvider {
    public void readOSM(OpenStreetMapContentHandler handler);

    /**
     * @see org.opentripplanner.graph_builder.services.GraphBuilderModule#checkInputs()
     */
    public void checkInputs();
}
