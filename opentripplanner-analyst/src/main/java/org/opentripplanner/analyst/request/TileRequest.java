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

package org.opentripplanner.analyst.request;

import org.geotools.geometry.Envelope2D;

public class TileRequest {

    public final Envelope2D bbox; // includes CRS
    public final int width; 
    public final int height; 
    public final String routerId;
    public final String routerId2;
    
    public TileRequest(Envelope2D bbox, Integer width, Integer height, String routerId, String routerId2) {
        this.bbox = bbox;
        this.width = width;
        this.height = height;
        this.routerId = routerId;
        if (routerId2 == null || routerId2.equals(routerId)) {
        	this.routerId2 = null;
        }
        else {
        	this.routerId2 = routerId2;
        }
    }
    
    public int hashCode() {
        return bbox.hashCode() * 42677 + width + height * 1307;
    }
    
    public boolean equals(Object other) {
        if (other instanceof TileRequest) {
            TileRequest that = (TileRequest) other;
            boolean eqPartial = this.bbox.equals(that.bbox) &&
                   this.width  == that.width   &&
                   this.height == that.height  &&
                   this.routerId.equals(that.routerId);
            if (this.routerId2 == null) {
                return eqPartial && this.routerId2 == that.routerId2;
            }
            else {
                return eqPartial && this.routerId2.equals(that.routerId2);
            }
        }
        return false;
    }
    
    public String toString() {
        return String.format("<tile request, bbox=%s width=%d height=%d routerId=%s routerId2=%s>", 
                bbox, width, height, routerId, routerId2);
    }
    
    // implement iterable to iterate over pixels?

}
