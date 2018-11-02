package org.opentripplanner.routing.vertextype;

/** Marker interface for temporary vertices */
public interface TemporaryVertex {
    public boolean isEndVertex();

    public void dispose();
}
