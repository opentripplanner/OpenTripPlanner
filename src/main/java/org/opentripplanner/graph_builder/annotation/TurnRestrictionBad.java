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

public class TurnRestrictionBad extends GraphBuilderAnnotation {

    private static final long serialVersionUID = 1L;

    public static final String FMT = "Bad turn restriction at relation %s. Reason: %s";
    public static final String HTMLFMT = "Bad turn restriction at relation <a href='http://www.openstreetmap.org/relation/%s'>%s</a>. Reason: %s";
    
    final long id;

    final String reason;

    public TurnRestrictionBad(long relationOSMID, String reason) {
        this.id = relationOSMID;
        this.reason = reason;
    }

    @Override
    public String getMessage() {
        return String.format(FMT, id, reason);
    }

    @Override
    public String getHTMLMessage() {
        return String.format(HTMLFMT, id, id, reason);
    }

}
