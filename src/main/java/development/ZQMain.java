package development;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.standalone.Router;
import org.opentripplanner.routing.impl.GraphPathFinder;
import org.opentripplanner.routing.spt.GraphPath;

import java.util.List;

public class ZQMain {
    public static void main(String[] args){
        System.out.println("Reading graph...");
        //read in the graph
        Graph graph = null;

        //create request
        System.out.println("Creating request...");
        String requestStr = "FROM:[lat=_, lon=_] TO:[lat=_, lon=_] TIME:_ MODE:_";
        RoutingRequest request = null;

        //create router
        System.out.println("Creating router...");
        Router router = null;


        //create graph path finder (PathService)
        System.out.println("Creating Graph Path Finder...");
        GraphPathFinder pathFinder = null;


        //path finder get paths
        System.out.println("Routing paths...");
        List<GraphPath> routedPaths = null;
//        List<GraphPath> routedPaths = pathFinder.getPaths(request);

    }
}
