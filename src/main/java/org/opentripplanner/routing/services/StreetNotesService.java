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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Edge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;

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
     * Notes for street edges. We need to synchronize access to the map as they will be concurrent
     * write access (notes for temporary splitted street edges).
     */
    private final SetMultimap<Edge, MatcherAndAlert> notesForEdge = Multimaps
            .synchronizedSetMultimap(HashMultimap.<Edge, MatcherAndAlert> create());

    public StreetNotesService() {
    }

    public void addNote(Edge edge, Alert note) {
        addNote(edge, note, ALWAYS_MATCHER);
    }

    public void addNotes(Edge edge, Collection<Alert> notes, NoteMatcher matcher) {
        if (notes == null || notes.isEmpty())
            return; // Prevent unnecessary clutter on the map
        for (Alert note : notes) {
            addNote(edge, note, matcher);
        }
    }

    public void addNote(Edge edge, Alert note, NoteMatcher matcher) {
        if (LOG.isDebugEnabled())
            LOG.debug("Adding note {} to {} with matcher {}", note, edge, matcher);
        notesForEdge.put(edge, new MatcherAndAlert(matcher, note));
    }

    public void copyNotes(Edge edgeFrom, Edge edgeTo) {
        Set<MatcherAndAlert> notes = notesForEdge.get(edgeFrom);
        if (notes != null && !notes.isEmpty())
            notesForEdge.putAll(edgeTo, notes);
    }

    public Set<Alert> getNotes(State state) {
        Edge edge = state.getBackEdge();
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

    public void removeNotes(Edge edge) {
        if (LOG.isDebugEnabled())
            LOG.debug("Removing notes for edge: {}", edge);
        notesForEdge.removeAll(edge);
    }
}
