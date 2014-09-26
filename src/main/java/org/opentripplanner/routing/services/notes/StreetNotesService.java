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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Edge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This service manage street edge notes. An edge note is an free-format alert (text) attached to an
 * edge, which is returned in the itinerary when this edge is used, and which *does not have any
 * impact on routing*. The last restriction is necessary as the edge do not know which notes it is
 * attached to (this to prevent having to store note lists in the edge, which is memory consuming as
 * only few edges will have notes).
 * 
 * The service owns a list of StreetNotesSource, with a single static one used for graph building.
 * "Dynamic" notes can be returned by classes implementing StreetNoteSource, added to this service
 * during startup.
 * 
 * Typical notes are: Toll (driving), unpaved surface (walk,bike), wheelchair notes...
 * 
 * Each note is attached to a matcher, whose responsibility is to determine if the note is relevant
 * for an edge, based on the itinerary state at this edge (the state after the edge has been
 * traversed, ie "state back edge"). Usually matcher will match note based on the mode (cycling,
 * driving) or if a wheelchair access is requested.
 * 
 * @author laurent
 */
public class StreetNotesService implements Serializable {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(StreetNotesService.class);

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

    private List<StreetNotesSource> sources = new ArrayList<>();

    private StaticStreetNotesSource staticNotesSource = new StaticStreetNotesSource();

    public StreetNotesService() {
        sources.add(staticNotesSource);
    }

    /**
     * Add a new note source. The list is not transient so any source added before the graph is
     * saved will be serialized!
     * 
     * @param source
     */
    public void addNotesSource(StreetNotesSource source) {
        sources.add(source);
    }

    /**
     * Return the set of notes applicable for this state / backedge pair.
     * 
     * @param state
     * @return The set of notes or null if empty.
     */
    public Set<Alert> getNotes(State state) {
        Edge edge = state.getBackEdge();
        Set<MatcherAndAlert> maas = new HashSet<MatcherAndAlert>();

        for (StreetNotesSource source : sources) {
            Set<MatcherAndAlert> maas2 = source.getNotes(edge);
            if (maas2 != null)
                maas.addAll(maas2);
        }
        if (maas == null || maas.isEmpty()) {
            return null;
        }

        Set<Alert> notes = new HashSet<Alert>(maas.size());
        for (MatcherAndAlert maa : maas) {
            if (maa.getMatcher().matches(state))
                notes.add(maa.getNote());
        }
        if (notes.isEmpty())
            return null;
        return notes;
    }

    public void addStaticNote(Edge edge, Alert note, NoteMatcher matcher) {
        staticNotesSource.addNote(edge, note, matcher);
    }

    public void removeStaticNotes(Edge edge) {
        staticNotesSource.removeNotes(edge);
    }
}
