package org.opentripplanner.api.standalone;

import org.opentripplanner.analyst.core.GeometryIndex;
import org.opentripplanner.analyst.request.SPTCache;
import org.opentripplanner.analyst.request.SampleFactory;
import org.opentripplanner.analyst.request.TileCache;
import org.opentripplanner.api.ws.PlanGenerator;
import org.opentripplanner.api.ws.services.MetadataService;
import org.opentripplanner.jsonp.JsonpCallbackFilter;
import org.opentripplanner.routing.algorithm.GenericAStar;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.DefaultRemainingWeightHeuristicFactoryImpl;
import org.opentripplanner.routing.impl.GraphServiceImpl;
import org.opentripplanner.routing.impl.RetryingPathServiceImpl;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.PathService;
import org.opentripplanner.routing.services.RemainingWeightHeuristicFactory;
import org.opentripplanner.routing.services.SPTService;

public class OTPConfigurator {
    
    private static final String DEFAULT_GRAPH_LOCATION = "/var/otp/graphs";
    
    public static OTPComponentProviderFactory fromCommandLineArguments(String[] args) {
        
        // The PathService which wraps the SPTService
        RetryingPathServiceImpl pathService = new RetryingPathServiceImpl();
        pathService.setFirstPathTimeout(10.0);
        pathService.setMultiPathTimeout(1.0);
        
        // An adapter to make Jersey see OTP as a dependency injection framework.
        // Associate our specific instances with their interface classes.
        OTPComponentProviderFactory cpf = new OTPComponentProviderFactory(); 
        cpf.bind(RoutingRequest.class);
        cpf.bind(PlanGenerator.class);
        cpf.bind(MetadataService.class);
        cpf.bind(JsonpCallbackFilter.class);
        cpf.bind(GraphService.class, makeGraphService(args));
        cpf.bind(SPTService.class, new GenericAStar());
        cpf.bind(PathService.class, pathService);
        cpf.bind(RemainingWeightHeuristicFactory.class, 
                new DefaultRemainingWeightHeuristicFactoryImpl()); 

        // Optional Analyst Modules
        cpf.bind(SPTCache.class, new SPTCache());
        cpf.bind(TileCache.class, new TileCache());
        cpf.bind(GeometryIndex.class, new GeometryIndex());
        cpf.bind(SampleFactory.class, new SampleFactory());
        
        // Perform field injection on bound instances and call post-construct methods
        cpf.doneBinding();        
        return cpf;         
        
    }

    private static GraphServiceImpl makeGraphService(String[] args) {
        GraphServiceImpl graphService = new GraphServiceImpl();
        if (args.length > 0)
            graphService.setPath(args[0]);
        else
            graphService.setPath(DEFAULT_GRAPH_LOCATION);
        if (args.length > 1)
            graphService.setDefaultRouterId(args[1]);
        return graphService;
    }

}
