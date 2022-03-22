package org.opentripplanner.routing.services.notes;

import org.opentripplanner.routing.graph.Edge;

import java.util.Set;

/**
 * A source of notes for edges.
 * 
 * @author laurent
 */
public interface StreetNotesSource {

    Set<MatcherAndStreetNote> getNotes(Edge edge);
}
