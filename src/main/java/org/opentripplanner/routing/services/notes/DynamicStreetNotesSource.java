package org.opentripplanner.routing.services.notes;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import java.util.Set;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.TemporaryPartialStreetEdge;

/**
 * A notes source of dynamic notes, Usually created and modified by a single GraphUpdater.
 *
 * @author hannesj
 */
public class DynamicStreetNotesSource implements StreetNotesSource {

  private static final long serialVersionUID = 1L;

  /**
   * Notes for street edges. Volatile in order to guarantee that the access to notesForEdge is
   * safe.
   */
  private volatile SetMultimap<Edge, MatcherAndStreetNote> notesForEdge = HashMultimap.create();

  public DynamicStreetNotesSource() {}

  /**
   * Return the set of notes applicable for this state / backedge pair.
   *
   * @param edge The edge for which the notes will be returned.
   * @return The set of notes or null if empty.
   */
  @Override
  public Set<MatcherAndStreetNote> getNotes(Edge edge) {
    /* If the edge is temporary, we look for notes in it's parent edge. */
    if (edge instanceof TemporaryPartialStreetEdge) {
      edge = ((TemporaryPartialStreetEdge) edge).getParentEdge();
    }
    Set<MatcherAndStreetNote> maas = notesForEdge.get(edge);
    if (maas == null || maas.isEmpty()) {
      return null;
    }
    return maas;
  }

  /*
   * Update the NotesSource with a new set of notes.
   */
  public void setNotes(SetMultimap<Edge, MatcherAndStreetNote> notes) {
    this.notesForEdge = notes;
  }
}
