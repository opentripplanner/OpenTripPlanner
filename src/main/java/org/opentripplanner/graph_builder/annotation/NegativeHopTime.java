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

import org.onebusaway.gtfs.model.StopTime;

public class NegativeHopTime extends GraphBuilderAnnotation {

    private static final long serialVersionUID = 1L;

    public static final String FMT = "Negative time hop between %s and %s; skipping the entire trip. " +
    		"This might be caused by the use of 00:xx instead of 24:xx for stoptimes after midnight.";
    
    public final StopTime st0, st1;
    
    public NegativeHopTime(StopTime st0, StopTime st1){
    	this.st0 = st0;
    	this.st1 = st1;
    }
    
    @Override
    public String getMessage() {
        return String.format(FMT, st0, st1);
    }

}
