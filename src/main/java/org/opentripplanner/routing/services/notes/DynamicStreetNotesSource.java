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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import org.opentripplanner.routing.edgetype.PartialStreetEdge;
import org.opentripplanner.routing.graph.Edge;

import java.util.Set;

/**
 * A notes source of dynamic notes, Usually created and modified by a single GraphUpdater.
 *
 * @author hannesj
 */
public class DynamicStreetNotesSource implements StreetNotesSource {

    private static final long serialVersionUID = 1L;

    /**
     * Notes for street edges. Volatile in order to guarantee that the access to notesForEdge is safe.
     */
    private volatile SetMultimap<Edge, MatcherAndAlert> notesForEdge = HashMultimap.create();

    public DynamicStreetNotesSource() {
    }

    /**
     * Return the set of notes applicable for this state / backedge pair.
     *
     * @param edge The edge for which the notes will be returned.
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

    /*
     * Update the NotesSource with a new set of notes.
     */
    public void setNotes(SetMultimap<Edge, MatcherAndAlert> notes){
        this.notesForEdge = notes;
    }
}
