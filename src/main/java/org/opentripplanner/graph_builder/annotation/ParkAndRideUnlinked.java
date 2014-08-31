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

package org.opentripplanner.graph_builder.annotation;

public class ParkAndRideUnlinked extends GraphBuilderAnnotation {

    private static final long serialVersionUID = 1L;

    public static final String FMT = "Park and ride '%s' (%d) not linked to any streets; it will not be usable.";
    public static final String HTMLFMT = "Park and ride <a href='http://www.openstreetmap.org/way/%d'>'%s' (%d)</a> not linked to any streets; it will not be usable.";
    
    final String name;
    final long osmId;
    
    public ParkAndRideUnlinked(String name, long osmId){
    	this.name = name;
    	this.osmId = osmId;
    }
    
    @Override
    public String getMessage() {
        return String.format(FMT, name, osmId);
    }

    @Override
    public String getHTMLMessage() {
        return String.format(HTMLFMT, osmId, name, osmId);
    }

}
