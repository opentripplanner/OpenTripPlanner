package org.opentripplanner.graph_builder.module.stopsAlerts;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.TransitStop;

public abstract class AbstractStopTester implements IStopTester{

    String type;
    
    public String getType(){
    	return this.type;
    }

    @Override
    abstract public boolean fulfillDemands(TransitStop ts, Graph graph);

}
