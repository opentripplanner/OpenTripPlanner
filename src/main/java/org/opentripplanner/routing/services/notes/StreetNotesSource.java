package org.opentripplanner.routing.services.notes;

import java.util.Set;
import org.opentripplanner.astar.model.Edge;

/**
 * A source of notes for edges.
 *
 * @author laurent
 */
public interface StreetNotesSource {
  Set<MatcherAndStreetNote> getNotes(Edge edge);
}
