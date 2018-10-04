package org.opentripplanner.openstreetmap.services;



public interface OpenStreetMapProvider {
    public void readOSM(OpenStreetMapContentHandler handler);

    /** @see GraphBuilder.checkInputs() */
    public void checkInputs();
}
