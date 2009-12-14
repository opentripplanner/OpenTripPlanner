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

package org.opentripplanner.api.model; 

import java.util.logging.Logger; 

import javax.xml.bind.annotation.XmlElement; 

import org.opentripplanner.util.Constants; 

/** 
* 
*/ 
public class Place {
    protected static final Logger LOGGER = Logger.getLogger(Place.class.getCanonicalName());

    public String name = null;

    public String stopId = "123";

    public Double lon = null;
    public Double lat = null;

    @XmlElement
    String getGeometry() {

        return Constants.GEO_JSON + lon + "," + lat + Constants.GEO_JSON_TAIL;
    }

    public Place() {
    }

    public Place(Double lon, Double lat, String name) {
        this.lon = lon;
        this.lat = lat;
        this.name = name;
    }

    public Place(Double lon, Double lat, String name, String stopId) {
        this(lon, lat, name);
        this.stopId = stopId;
    }

}
