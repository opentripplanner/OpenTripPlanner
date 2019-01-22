package org.opentripplanner.routing.graph;

/**
 * Seems to be used only in tests. As far as I know this is not used in normal routing (abyrd).
 */
public class SimpleConcreteVertex extends Vertex {

    private static final long serialVersionUID = 1L;

    public SimpleConcreteVertex(Graph g, String label, double lat, double lon) {
        super(g, label, lon, lat);
    }
}