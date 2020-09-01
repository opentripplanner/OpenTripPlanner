package org.opentripplanner.routing.services.notes;

import java.util.Set;

import org.opentripplanner.routing.graph.Edge;

/**
 * A source of notes for edges.
 * 
 * @author laurent
 */
public interface StreetNotesSource {

    public Set<MatcherAndStreetNote> getNotes(Edge edge);
}
