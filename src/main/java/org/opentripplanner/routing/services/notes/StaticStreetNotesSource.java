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

package org.opentripplanner.routing.services.notes;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.opentripplanner.common.model.T2;
import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.PartialStreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

/**
 * A notes source of static notes, usually created at graph building stage and not modified
 * thereafter.
 * 
 * @author laurent
 */
public class StaticStreetNotesSource implements StreetNotesSource, Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(StaticStreetNotesSource.class);

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

    StaticStreetNotesSource() {
    }

    void addNote(Edge edge, Alert note, NoteMatcher matcher) {
        if (LOG.isDebugEnabled())
            LOG.debug("Adding note {} to {} with matcher {}", note, edge, matcher);
        notesForEdge.put(edge, buildMatcherAndAlert(matcher, note));
    }

    /**
     * Return the set of notes applicable for this state / backedge pair.
     * @return The set of notes or null if empty.
     */
    @Override
    public Set<MatcherAndAlert> getNotes(Edge edge) {
        /* If the edge is temporary, we look for notes in it's parent edge. */
        if (edge instanceof PartialStreetEdge) {
            edge = ((PartialStreetEdge) edge).getParentEdge();
        }
        Set<MatcherAndAlert> maas = notesForEdge.get(edge);
        if (maas == null || maas.isEmpty()) {
            return null;
        }
        return maas;
    }

    /**
     * Remove all notes attached to this edge. NOTE: this should only be called within a graph
     * building context (or unit testing).
     * 
     * @param edge
     */
    void removeNotes(Edge edge) {
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
