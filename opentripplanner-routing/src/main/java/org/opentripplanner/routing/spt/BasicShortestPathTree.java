package org.opentripplanner.routing.spt;

import java.util.Collection;
import java.util.HashMap;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;

public class BasicShortestPathTree implements ShortestPathTree {
    private static final long serialVersionUID = -3899613853043676031L;

    HashMap<Vertex, SPTVertex> vertices;

    public BasicShortestPathTree() {
        vertices = new HashMap<Vertex, SPTVertex>();
    }

    public SPTVertex addVertex(Vertex vv, State ss, double weightSum, TraverseOptions options) {
        SPTVertex ret = this.vertices.get(vv);
        if (ret == null) {
            ret = new SPTVertex(vv, ss, weightSum, options);
            this.vertices.put(vv, ret);
        } else {
            if (weightSum < ret.weightSum) {
                ret.weightSum = weightSum;
                ret.state = ss;
                ret.options = options;
            }
        }
        return ret;
    }

    public Collection<SPTVertex> getVertices() {
        return this.vertices.values();
    }

    public SPTVertex getVertex(Vertex vv) {
        return (SPTVertex) this.vertices.get(vv);
    }

    public GraphPath getPath(Vertex dest) {
        return getPath(dest, true);
    }
    
    public GraphPath getPath(Vertex dest, boolean optimize) {
        SPTVertex end = this.getVertex(dest);
        if (end == null) {
            return null;
        }

        GraphPath ret = new GraphPath();
        while (true) {
            ret.vertices.add(0, end);
            if (end.incoming == null) {
                break;
            }
            ret.edges.add(0, end.incoming);
            end = end.incoming.fromv;
        }
        if (optimize) {
            ret.optimize();
        }
        return ret;
    }

    public String toString() {
        return "SPT " + this.vertices.size();
    }

    @Override
    public void removeVertex(SPTVertex vertex) {
        vertices.remove(vertex.mirror);
    }
}
