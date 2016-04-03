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

import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStop;

public class StopNotLinkedForTransfers extends GraphBuilderAnnotation {
    private static final long serialVersionUID = 1L;

    public static final String FMT = "Stop %s not near any other stops; no transfers are possible.";

    public static final String HTMLFMT = "Stop <a href=\"http://www.openstreetmap.org/?mlat=%s&mlon=%s&layers=T\">\"%s (%s)\"</a> not near any other stops; no transfers are possible.";

    final TransitStop stop;
    
    public StopNotLinkedForTransfers(TransitStop stop){
    	this.stop = stop;
    }

    @Override
    public String getHTMLMessage() {
        return String.format(HTMLFMT, stop.getLat(), stop.getLon(), stop.getName(), stop.getStopId());
    }

    @Override
    public String getMessage() {
        return String.format(FMT, stop);
    }

    @Override
    public Vertex getReferencedVertex() {
        return this.stop;
    }
}
