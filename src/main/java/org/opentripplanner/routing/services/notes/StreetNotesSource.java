package org.opentripplanner.routing.services.notes;

import java.util.Set;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.note.StreetNoteAndMatcher;

/**
 * A source of notes for edges.
 *
 * @author laurent
 */
public interface StreetNotesSource {
  Set<StreetNoteAndMatcher> getNotes(Edge edge);
}
