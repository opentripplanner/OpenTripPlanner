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

package org.opentripplanner.api.model.patch;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import org.opentripplanner.routing.patch.Patch;
import org.opentripplanner.routing.patch.RouteNotePatch;
import org.opentripplanner.routing.patch.StopNotePatch;

@XmlRootElement(name="PatchSet")
public class PatchSet {
	@XmlElements({
	    @XmlElement(name = "StopNotePatch", type = StopNotePatch.class),
	    @XmlElement(name = "RouteNotePatch", type = RouteNotePatch.class)
	    })
	public List<Patch> patches;
	
}
