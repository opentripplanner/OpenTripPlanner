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

package org.opentripplanner.routing.patch;

import java.util.HashSet;
import java.util.Set;

import org.opentripplanner.routing.core.EdgeNarrative;
import org.opentripplanner.routing.edgetype.DelegatingEdgeNarrative;

public class NoteNarrative extends DelegatingEdgeNarrative {

	private Alert note;

	public NoteNarrative(EdgeNarrative base, Alert notes) {
		super(base);
		this.note = notes;
	}
	
	@Override
	public Set<Alert> getNotes() {
		Set<Alert> baseNotes = base.getNotes();
		HashSet<Alert> notes;
		if (baseNotes != null) {
			 notes = new HashSet<Alert>(baseNotes);
		} else {
			notes = new HashSet<Alert>(1);
		}
		notes.add(note);
		return notes;
	}
}