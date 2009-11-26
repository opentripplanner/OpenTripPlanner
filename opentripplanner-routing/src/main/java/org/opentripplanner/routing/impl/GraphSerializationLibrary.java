package org.opentripplanner.routing.impl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.opentripplanner.routing.core.AbstractEdge;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphSerializationLibrary {
    
    private static Logger _log = LoggerFactory.getLogger(GraphSerializationLibrary.class);

    public static void writeGraph(Graph graph, File graphPath) throws IOException {

        if (!graphPath.getParentFile().exists())
            graphPath.getParentFile().mkdirs();

        _log.info("Writing graph...");
        ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(graphPath)));
        out.writeObject(graph);
        out.close();
        _log.info("Graph written");
    }

    public static Graph readGraph(File graphPath) throws IOException, ClassNotFoundException {
        ObjectInputStream in = new ObjectInputStream(new BufferedInputStream (new FileInputStream(graphPath)));
        _log.info("Reading graph...");
        Graph graph = (Graph) in.readObject();
        _log.info("Initializing graph...");
        for (Vertex vertex : graph.getVertices()) {
            for (Edge edge : vertex.getIncoming())
                visitEdge(edge, graph);
            for (Edge edge : vertex.getOutgoing())
                visitEdge(edge, graph);
        }
        _log.info("Graph read");
        return graph;
    }

    private static void visitEdge(Edge edge, Graph graph) {
        if (!(edge instanceof AbstractEdge))
            return;
        AbstractEdge ae = (AbstractEdge) edge;
        ae.replaceDummyVertices(graph);
    }
}
