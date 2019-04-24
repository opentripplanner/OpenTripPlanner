package org.opentripplanner.ext.examples.statistics.api;


public class GraphStatistics {
    private int nStopsInGraph;
    private int nStationsInGraph;

    public GraphStatistics(int nStopsInGraph, int nStationsInGraph) {
        this.nStopsInGraph = nStopsInGraph;
        this.nStationsInGraph = nStationsInGraph;
    }

    public int getnStopsInGraph() {
        return nStopsInGraph;
    }

    public void setnStopsInGraph(int nStopsInGraph) {
        this.nStopsInGraph = nStopsInGraph;
    }

    public int getnStationsInGraph() {
        return nStationsInGraph;
    }

    public void setnStationsInGraph(int nStationsInGraph) {
        this.nStationsInGraph = nStationsInGraph;
    }

    @Override public String toString() {
        return "GraphStatistics{" + "nStopsInGraph=" + nStopsInGraph + ", nStationsInGraph="
                + nStationsInGraph + '}';
    }
}
