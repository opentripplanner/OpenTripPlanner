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

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.StopTime;

public class BogusShapeGeometryCaught extends GraphBuilderAnnotation {

    private static final long serialVersionUID = 1L;

    public static final String FMT = "Shape geometry for shape_id %s cannot be used with stop " +
    		"times %s and %s; using straight-line path instead";
    
    final AgencyAndId shapeId;
    final StopTime stA;
    final StopTime stB;
    
    public BogusShapeGeometryCaught(AgencyAndId shapeId, StopTime stA, StopTime stB){
    	this.shapeId = shapeId;
    	this.stA = stA;
    	this.stB = stB;
    }
    
    @Override
    public String getMessage() {
        return String.format(FMT, shapeId, stA, stB);
    }

}
