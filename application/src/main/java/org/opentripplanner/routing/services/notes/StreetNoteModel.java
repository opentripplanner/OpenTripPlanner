package org.opentripplanner.routing.services.notes;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.TemporaryPartialStreetEdge;
import org.opentripplanner.street.model.note.StreetNote;
import org.opentripplanner.street.model.note.StreetNoteAndMatcher;
import org.opentripplanner.street.model.note.StreetNoteMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A notes source of static notes, created at graph building stage and not modified
 * thereafter.
 */
public class StreetNoteModel implements Serializable {

  private static final Logger LOG = LoggerFactory.getLogger(StreetNoteModel.class);

  /**
   * Notes for street edges. No need to synchronize access to the map as they will not be concurrent
   * write access (no notes for temporary edges, we use notes from parent).
   */
  private final SetMultimap<Edge, StreetNoteAndMatcher> notesForEdge = HashMultimap.<
      Edge,
      StreetNoteAndMatcher
    >create();

  /**
   * Set of unique matchers, kept during building phase, used for interning (lots of note/matchers
   * are identical).
   */
  private final transient Map<StreetNoteAndMatcher, StreetNoteAndMatcher> uniqueMatchers =
    new HashMap<>();

  StreetNoteModel() {}

  /**
   * Return the set of notes applicable for this state / backedge pair.
   *
   * @return The set of notes or null if empty.
   */
  public Set<StreetNoteAndMatcher> getNotes(Edge edge) {
    /* If the edge is temporary, we look for notes in it's parent edge. */
    if (edge instanceof TemporaryPartialStreetEdge) {
      edge = ((TemporaryPartialStreetEdge) edge).getParentEdge();
    }
    Set<StreetNoteAndMatcher> maas = notesForEdge.get(edge);
    if (maas == null || maas.isEmpty()) {
      return null;
    }
    return maas;
  }

  void addNote(Edge edge, StreetNote note, StreetNoteMatcher matcher) {
    if (LOG.isDebugEnabled()) LOG.debug(
      "Adding note {} to {} with matcher {}",
      note,
      edge,
      matcher
    );
    notesForEdge.put(edge, buildMatcherAndAlert(matcher, note));
  }

  /**
   * Remove all notes attached to this edge. NOTE: this should only be called within a graph
   * building context (or unit testing).
   */
  void removeNotes(Edge edge) {
    if (LOG.isDebugEnabled()) LOG.debug("Removing notes for edge: {}", edge);
    notesForEdge.removeAll(edge);
  }

  /**
   * Create a MatcherAndAlert, interning it if the note and matcher pair is already created. Note:
   * we use the default Object.equals() for matchers, as they are mostly already singleton
   * instances.
   */
  private StreetNoteAndMatcher buildMatcherAndAlert(
    StreetNoteMatcher noteMatcher,
    StreetNote note
  ) {
    var candidate = new StreetNoteAndMatcher(note, noteMatcher);
    var interned = uniqueMatchers.putIfAbsent(candidate, candidate);
    return interned == null ? candidate : interned;
  }
}
