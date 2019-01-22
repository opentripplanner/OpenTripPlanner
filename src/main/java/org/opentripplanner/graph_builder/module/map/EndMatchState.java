package org.opentripplanner.graph_builder.module.map;

import java.util.List;

/** 
 * The end of a route's geometry, meaning that the search can quit
 * @author novalis
 *
 */
public class EndMatchState extends MatchState {

    public EndMatchState(MatchState parent, double error, double distance) {
        super(parent, null, distance);
        this.currentError = error;
    }

    @Override
    public List<MatchState> getNextStates() {
        return null;
    }

}
