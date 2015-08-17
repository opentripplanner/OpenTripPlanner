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

public class TurnRestrictionException extends GraphBuilderAnnotation {

    private static final long serialVersionUID = 1L;

    public static final String FMT = "Turn restriction with bicycle exception at node %s from %s";

    public static final String HTMLFMT = "Turn restriction with bicycle exception at node <a href=\"http://www.openstreetmap.org/node/%d\">\"%d\"</a> from <a href=\"http://www.openstreetmap.org/way/%d\">\"%d\"</a>";
    
    final long nodeId, wayId;
    
    public TurnRestrictionException(long nodeId, long wayId){
    	this.nodeId = nodeId;
    	this.wayId = wayId;
    }

    @Override
    public String getHTMLMessage() {
        return String.format(HTMLFMT, nodeId, nodeId, wayId, wayId);
    }

    @Override
    public String getMessage() {
        return String.format(FMT, nodeId, wayId);
    }

}
