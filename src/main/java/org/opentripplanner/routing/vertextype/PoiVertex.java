package org.opentripplanner.routing.vertextype;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    Map<String, String> accessibilityViewpoints = new HashMap<>();

    @Getter
    List<String> categories = new ArrayList<>();

    public PoiVertex(Graph g, String label, double x, double y, String name) {
        super(g, "poi:" + label, x, y, name);
    }

    public void setAccessibilityViewpoints(String accessibility) {
        for (String accessibilityString : accessibility.split(",")) {
            this.accessibilityViewpoints.put(accessibilityString.split(":")[0],
                    accessibilityString.split(":")[1]);
        }
    }

    public void setCategories(List<Object> list){
        for (Object o: list){
            categories.add("poi:category:" + o.toString());
        }
    }

}
