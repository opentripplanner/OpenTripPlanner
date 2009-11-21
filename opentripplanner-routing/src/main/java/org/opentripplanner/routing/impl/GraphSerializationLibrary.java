package org.opentripplanner.routing.impl;

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

public class GraphSerializationLibrary {

    public static void writeGraph(Graph graph, File graphPath) throws IOException {

        if (!graphPath.getParentFile().exists())
            graphPath.getParentFile().mkdirs();

        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(graphPath));
        out.writeObject(graph);
        out.close();
    }

    public static Graph readGraph(File graphPath) throws IOException, ClassNotFoundException {
        ObjectInputStream in = new ObjectInputStream(new FileInputStream(graphPath));
        Graph graph = (Graph) in.readObject();
        for (Vertex vertex : graph.getVertices()) {
            for (Edge edge : vertex.getIncoming())
                visitEdge(edge, graph);
            for (Edge edge : vertex.getOutgoing())
                visitEdge(edge, graph);
        }

        return graph;
    }

    private static void visitEdge(Edge edge, Graph graph) {
        if (!(edge instanceof AbstractEdge))
            return;
        AbstractEdge ae = (AbstractEdge) edge;
        ae.replaceDummyVertices(graph);
    }
}
