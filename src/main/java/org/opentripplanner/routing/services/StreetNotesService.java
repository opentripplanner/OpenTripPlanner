/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (props, at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.services;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.opentripplanner.common.model.T2;
import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.PartialPlainStreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

/**
 * This service is a manager / index for edge notes. An edge note is an free-format alert attached
 * to an edge, which is returned in the itinerary when this edge is used, and which *does not have
 * any impact on routing*. The last restriction is necessary as the edge do not know which notes it
 * is attached to (this to prevent having to store note lists in the edge, which is memory consuming
 * as only few notes will have notes).
 * 
 * Typical notes are: Toll (driving), unpaved surface (walk,bike), wheelchair notes...
 * 
 * Each note is attached to a matcher, whose responsibility is to determine if the note is relevant
 * for an edge, based on the itinerary state at this edge (the state after the edge has been
 * traversed). Usually matcher will match note based on the mode (cycling, driving) or if a
 * wheelchair access is requested.
 * 
 * @author laurent
 */
public class StreetNotesService implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(StreetNotesService.class);

    public interface NoteMatcher extends Serializable {
        public boolean matches(State state);
    }

    public static final NoteMatcher WHEELCHAIR_MATCHER = new NoteMatcher() {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean matches(State state) {
            return state.getOptions().wheelchairAccessible;
        }
    };

    public static final NoteMatcher DRIVING_MATCHER = new NoteMatcher() {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean matches(State state) {
            return state.getBackMode().isDriving();
        }
    };

    public static final NoteMatcher BICYCLE_MATCHER = new NoteMatcher() {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean matches(State state) {
            return state.getBackMode() == TraverseMode.BICYCLE;
        }
    };

    public static final NoteMatcher ALWAYS_MATCHER = new NoteMatcher() {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean matches(State state) {
            return true;
        }
    };

    private static class MatcherAndAlert implements Serializable {
        private static final long serialVersionUID = 1L;

        private MatcherAndAlert(NoteMatcher matcher, Alert note) {
            this.matcher = matcher;
            this.note = note;
        }

        private NoteMatcher matcher;

        private Alert note;
    }

    /**
     * Notes for street edges. No need to synchronize access to the map as they will not be
     * concurrent write access (no notes for temporary edges, we use notes from parent).
     */
    private final SetMultimap<Edge, MatcherAndAlert> notesForEdge = HashMultimap
            .<Edge, MatcherAndAlert> create();

    /**
     * Set of unique matchers, kept during building phase, used for interning (lots of note/matchers
     * are identical).
     */
    private transient Map<T2<NoteMatcher, Alert>, MatcherAndAlert> uniqueMatchers = new HashMap<>();

    public StreetNotesService() {
    }

    public void addNote(Edge edge, Alert note) {
        addNote(edge, note, ALWAYS_MATCHER);
    }

    public void addNote(Edge edge, Alert note, NoteMatcher matcher) {
        if (LOG.isDebugEnabled())
            LOG.debug("Adding note {} to {} with matcher {}", note, edge, matcher);
        notesForEdge.put(edge, buildMatcherAndAlert(matcher, note));
    }

    /**
     * Return the set of notes applicable for this state / backedge pair.
     * 
     * @param state
     * @return The set of notes or null if empty.
     */
    public Set<Alert> getNotes(State state) {
        Edge edge = state.getBackEdge();
        if (edge instanceof PartialPlainStreetEdge) {
            edge = ((PartialPlainStreetEdge) edge).getParentEdge();
        }
        Set<MatcherAndAlert> maas = notesForEdge.get(edge);
        if (maas == null || maas.isEmpty()) {
            return null;
        }
        Set<Alert> notes = new HashSet<Alert>(maas.size());
        for (MatcherAndAlert maa : maas) {
            if (maa.matcher.matches(state))
                notes.add(maa.note);
        }
        if (notes.isEmpty())
            return null;
        return notes;
    }

    /**
     * Remove all notes attached to this edge. NOTE: this should only be called within a graph
     * building context (or unit testing).
     * 
     * @param edge
     */
    public void removeNotes(Edge edge) {
        if (LOG.isDebugEnabled())
            LOG.debug("Removing notes for edge: {}", edge);
        notesForEdge.removeAll(edge);
    }

    /**
     * Create a MatcherAndAlert, interning it if the note and matcher pair is already created. Note:
     * we use the default Object.equals() for matchers, as they are mostly already singleton
     * instances.
     * 
     * @param noteMatcher
     * @param note
     * @return
     */
    private MatcherAndAlert buildMatcherAndAlert(NoteMatcher noteMatcher, Alert note) {
        T2<NoteMatcher, Alert> key = new T2<>(noteMatcher, note);
        MatcherAndAlert interned = uniqueMatchers.get(key);
        if (interned != null) {
            return interned;
        }
        MatcherAndAlert ret = new MatcherAndAlert(noteMatcher, note);
        uniqueMatchers.put(key, ret);
        return ret;
    }
}
