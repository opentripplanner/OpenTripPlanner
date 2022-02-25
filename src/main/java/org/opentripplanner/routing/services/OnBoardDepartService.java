package org.opentripplanner.routing.services;

import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.graph.Vertex;

import java.io.Serializable;

/**
 * Graph service for depart-on-board mode.
 * 
 * @author laurent
 */
public interface OnBoardDepartService extends Serializable {

    public abstract Vertex setupDepartOnBoard(RoutingContext ctx);
}
