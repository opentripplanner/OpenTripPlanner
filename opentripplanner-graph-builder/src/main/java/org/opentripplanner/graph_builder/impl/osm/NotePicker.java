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

package org.opentripplanner.graph_builder.impl.osm;

/**
 * Defines which OSM ways get notes and what kind of notes they get.
 * 
 * @author novalis
 * 
 */
public class NotePicker {

	private OSMSpecifier specifier;
	private NoteProperties noteProperties;

	public NotePicker(OSMSpecifier specifier, NoteProperties noteProperties) {
		this.specifier = specifier;
		this.noteProperties = noteProperties;
	}

	public void setSpecifier(OSMSpecifier specifier) {
		this.specifier = specifier;
	}

	public OSMSpecifier getSpecifier() {
		return specifier;
	}

	public void setNoteProperties(NoteProperties noteProperties) {
		this.noteProperties = noteProperties;
	}

	public NoteProperties getNoteProperties() {
		return noteProperties;
	}

}
