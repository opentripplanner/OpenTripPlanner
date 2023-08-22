package org.opentripplanner.routing.services.notes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.note.StreetNote;
import org.opentripplanner.street.model.note.StreetNoteAndMatcher;
import org.opentripplanner.street.model.note.StreetNoteMatcher;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.state.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This service manage street edge notes. An edge note is an free-format alert (text) attached to an
 * edge, which is returned in the itinerary when this edge is used, and which *does not have any
 * impact on routing*. The last restriction is necessary as the edge do not know which notes it is
 * attached to (this to prevent having to store note lists in the edge, which is memory consuming as
 * only few edges will have notes).
 * <p>
 * The service owns a list of StreetNotesSource, with a single static one used for graph building.
 * "Dynamic" notes can be returned by classes implementing StreetNoteSource, added to this service
 * during startup.
 * <p>
 * Typical notes are: Toll (driving), unpaved surface (walk,bike), wheelchair notes...
 * <p>
 * Each note is attached to a matcher, whose responsibility is to determine if the note is relevant
 * for an edge, based on the itinerary state at this edge (the state after the edge has been
 * traversed, ie "state back edge"). Usually matcher will match note based on the mode (cycling,
 * driving) or if a wheelchair access is requested.
 *
 * @author laurent
 */
public class StreetNotesService implements Serializable {

  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger(StreetNotesService.class);

  public static final StreetNoteMatcher WHEELCHAIR_MATCHER = new StreetNoteMatcher() {
    @Override
    public boolean matches(State state) {
      return state.getRequest().wheelchair();
    }
  };

  public static final StreetNoteMatcher DRIVING_MATCHER = new StreetNoteMatcher() {
    @Override
    public boolean matches(State state) {
      return state.getBackMode().isInCar();
    }
  };

  public static final StreetNoteMatcher BICYCLE_MATCHER = new StreetNoteMatcher() {
    @Override
    public boolean matches(State state) {
      return state.getBackMode() == TraverseMode.BICYCLE;
    }
  };

  public static final StreetNoteMatcher ALWAYS_MATCHER = new StreetNoteMatcher() {
    @Override
    public boolean matches(State state) {
      return true;
    }
  };

  private final List<StreetNoteModel> sources = new ArrayList<>();

  private final StreetNoteModel staticNotesSource = new StreetNoteModel();

  public StreetNotesService() {
    sources.add(staticNotesSource);
  }

  /**
   * Add a new note source. The list is not transient so any source added before the graph is saved
   * will be serialized!
   */
  public void addNotesSource(StreetNoteModel source) {
    sources.add(source);
  }

  /**
   * Return the set of notes applicable for this state / backedge pair.
   *
   * @return The set of notes or null if empty.
   */
  public Set<StreetNote> getNotes(State state) {
    Edge edge = state.getBackEdge();
    Set<StreetNoteAndMatcher> maas = new HashSet<>();

    for (StreetNoteModel source : sources) {
      Set<StreetNoteAndMatcher> maas2 = source.getNotes(edge);
      if (maas2 != null) maas.addAll(maas2);
    }
    if (maas == null || maas.isEmpty()) {
      return Set.of();
    }

    Set<StreetNote> notes = new HashSet<>(maas.size());
    for (StreetNoteAndMatcher maa : maas) {
      if (maa.matcher().matches(state)) {
        notes.add(maa.note());
      }
    }
    if (notes.isEmpty()) {
      return Set.of();
    }
    return notes;
  }

  public void addStaticNote(Edge edge, StreetNote note, StreetNoteMatcher matcher) {
    staticNotesSource.addNote(edge, note, matcher);
  }

  public void removeStaticNotes(Edge edge) {
    staticNotesSource.removeNotes(edge);
  }
}
