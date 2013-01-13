package org.opentripplanner.graph_builder.impl.stopsAlerts;

import lombok.Getter;
import lombok.Setter;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.TransitStop;

/**
 * Created with IntelliJ IDEA.
 * User: Ben
 * Date: 02/01/13
 * Time: 17:22
 * To change this template use File | Settings | File Templates.
 */
public abstract class AbstractStopTester implements IStopTester{

    @Getter @Setter
    String type;

    @Override
    abstract public boolean fulfillDemands(TransitStop ts, Graph graph);

}
