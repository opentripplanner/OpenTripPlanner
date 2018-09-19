package org.opentripplanner.serializer;

import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;

public class JavaGraphSerializer implements GraphSerializer {

    private static final Logger LOG = LoggerFactory.getLogger(JavaGraphSerializer.class);

    @Override
    public GraphWrapper deserialize(InputStream inputStream) {
        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);

            GraphWrapper graphWrapper = new GraphWrapper();

            LOG.debug("Loading graph...");
            graphWrapper.graph = (Graph) objectInputStream.readObject();

            LOG.debug("Loading edges...");
            graphWrapper.edges = (ArrayList<Edge>) objectInputStream.readObject();

            return graphWrapper;

        } catch (IOException e) {
            throw new GraphSerializationException("Cannot deserialize incoming date", e);
        } catch (ClassNotFoundException ex) {
            LOG.error("Stored graph is incompatible with this version of OTP, please rebuild it.");
            throw new GraphSerializationException("Stored Graph version error", ex);
        }
    }

    @Override
    public void serialize(GraphWrapper graphWrapper, OutputStream outputStream) throws GraphSerializationException {

        try {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);

            LOG.debug("Writing graph...");
            objectOutputStream.writeObject(graphWrapper.graph);

            LOG.debug("Writing edges...");
            objectOutputStream.writeObject(graphWrapper.edges);

            outputStream.close();
            LOG.info("Graph written.");
        } catch (IOException e) {
            throw new GraphSerializationException("Could not write graph", e);
        }

    }
}
