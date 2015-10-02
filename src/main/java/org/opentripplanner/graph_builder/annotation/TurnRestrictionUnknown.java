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

public class TurnRestrictionUnknown extends GraphBuilderAnnotation {

    private static final long serialVersionUID = 1L;

    public static final String FMT = "Invalid turn restriction tag %s in turn restriction %d";

    public static final String HTMLFMT = "Invalid turn restriction tag %s in  <a href=\"http://www.openstreetmap.org/relation/%d\">\"%d\"</a>";
    
    final String tagval;
    final long relationId;
    
    public TurnRestrictionUnknown(long relationId, String tagval){
        this.relationId = relationId;
    	this.tagval = tagval;
    }

    @Override
    public String getHTMLMessage() {
        return String.format(FMT, tagval, relationId, relationId);
    }

    @Override
    public String getMessage() {
        return String.format(FMT, tagval, relationId);
    }

}
