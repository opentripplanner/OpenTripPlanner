package org.opentripplanner.api.ws.services;

import java.util.HashMap;

import org.opentripplanner.api.ws.GraphMetadata;
import org.opentripplanner.routing.services.GraphService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MetadataService {
    @Autowired
    private GraphService graphService;

    HashMap<String, GraphMetadata> metadata = new HashMap<String, GraphMetadata>();
    
    public synchronized GraphMetadata getMetadata(String routerId) {
        GraphMetadata data = metadata.get(routerId);
        if (data == null) {
            data = new GraphMetadata(graphService.getGraph(routerId));
            metadata.put(routerId, data);
        }
        return data;
    }

    public GraphService getGraphService() {
        return graphService;
    }

    public void setGraphService(GraphService graphService) {
        this.graphService = graphService;
    }

}
