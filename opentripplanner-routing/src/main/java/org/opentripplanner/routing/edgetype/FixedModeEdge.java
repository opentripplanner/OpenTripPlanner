/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.edgetype;

import java.util.HashSet;
import java.util.Set;

import org.opentripplanner.routing.core.EdgeNarrative;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.patch.Alert;

public class FixedModeEdge extends DelegatingEdgeNarrative {

    private TraverseMode mode;
    private Set<Alert> extraNotes;

    public FixedModeEdge(EdgeNarrative base, TraverseMode mode) {
        super(base);
        this.mode = mode;
    }

    @Override
    public TraverseMode getMode() {
        return mode;
    }

    public String toString() {
        return "FixedModeEdge(" + base + ", " + mode + ")";
    }

    public void addNotes(Set<Alert> notes) {
        this.extraNotes = notes;
    }

    @Override
    public Set<Alert> getNotes() {
        Set<Alert> notes = base.getNotes();
        if (notes == null) {
            return extraNotes;
        }
        if (extraNotes == null) {
            return notes;
        }
        notes = new HashSet<Alert>(notes);
        notes.addAll(extraNotes);
        return notes;
    }
}