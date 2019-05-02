package org.opentripplanner.ext.examples.statistics.api;


public class GraphStatistics {
    private int nStopsInGraph;
    private int nStationsInGraph;

    public GraphStatistics(int nStopsInGraph, int nStationsInGraph) {
        this.nStopsInGraph = nStopsInGraph;
        this.nStationsInGraph = nStationsInGraph;
    }

    public int getNStopsInGraph() {
        return nStopsInGraph;
    }

    public void setNStopsInGraph(int nStopsInGraph) {
        this.nStopsInGraph = nStopsInGraph;
    }

    public int getNStationsInGraph() {
        return nStationsInGraph;
    }

    public void setNStationsInGraph(int nStationsInGraph) {
        this.nStationsInGraph = nStationsInGraph;
    }

    @Override public String toString() {
        return "GraphStatistics{" + "nStopsInGraph=" + nStopsInGraph + ", nStationsInGraph="
                + nStationsInGraph + '}';
    }
}
