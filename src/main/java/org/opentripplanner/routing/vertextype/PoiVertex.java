package org.opentripplanner.routing.vertextype;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

import org.opentripplanner.common.MavenVersion;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;

public class PoiVertex extends Vertex {

    private static final long serialVersionUID = MavenVersion.VERSION.getUID();

    @Getter
    @Setter
    Map<String, String> accessibility_viewpoints = new HashMap<>();

    public PoiVertex(Graph g, String label, double x, double y, String name,
                     String accessibility_viewpoints) {
        super(g, label, x, y, name);
        for (String accessibility_string : accessibility_viewpoints.split(",")) {
            this.accessibility_viewpoints.put(accessibility_string.split(":")[0],
                    accessibility_string.split(":")[1]);
        }
    }

    public PoiVertex(Graph g, String label, double x, double y, String name) {
        super(g, label, x, y, name);
    }
}
