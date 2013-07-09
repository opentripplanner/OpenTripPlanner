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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.opentripplanner.model.json_serialization.GeoJSONDeserializer;
import org.opentripplanner.model.json_serialization.GeoJSONSerializer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.vividsolutions.jts.geom.Geometry;

@XmlRootElement(name = "RouterInfo")
public class RouterInfo {
    @XmlElement
    public String routerId;
    
    @JsonSerialize(using=GeoJSONSerializer.class)
    @JsonDeserialize(using=GeoJSONDeserializer.class)
    @XmlJavaTypeAdapter(value=GeometryAdapter.class,type=Geometry.class)
    public Geometry polygon;
}
