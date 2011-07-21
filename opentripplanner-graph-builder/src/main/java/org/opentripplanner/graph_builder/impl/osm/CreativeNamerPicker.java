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

public class CreativeNamerPicker {
	private OSMSpecifier specifier;
	private CreativeNamer namer;

	public CreativeNamerPicker() {
	}

	public CreativeNamerPicker(OSMSpecifier specifier, CreativeNamer namer) {
		this.specifier = specifier;
		this.namer = namer;
	}

	public void setSpecifier(OSMSpecifier specifier) {
		this.specifier = specifier;
	}

	public OSMSpecifier getSpecifier() {
		return specifier;
	}

	public void setNamer(CreativeNamer namer) {
		this.namer = namer;
	}

	public CreativeNamer getNamer() {
		return namer;
	}

}
