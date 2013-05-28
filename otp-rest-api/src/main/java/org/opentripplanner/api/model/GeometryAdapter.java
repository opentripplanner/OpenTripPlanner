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

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.opentripplanner.common.geometry.GeometryUtils;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.gml2.GMLWriter;

public class GeometryAdapter extends XmlAdapter<String,Geometry> {
    public Geometry unmarshal(String val) throws Exception {
        return new WKTReader(GeometryUtils.getGeometryFactory()).read(val);
    }
    public String marshal(Geometry val) throws Exception {
        GMLWriter writer = new GMLWriter();
        writer.setNamespace(false);
        return writer.write(val);
    }
}