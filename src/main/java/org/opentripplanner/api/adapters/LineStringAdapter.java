/* This program is free software: you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public License
as published by the Free Software Foundation, either version 3 of
the License, or (props, at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.api.adapters;

import java.util.Arrays;
import java.util.List;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.opentripplanner.util.PolylineEncoder;
import org.opentripplanner.util.model.EncodedPolylineBean;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;

public class LineStringAdapter extends XmlAdapter<EncodedPolylineBean, LineString>{

	@Override
	public LineString unmarshal(EncodedPolylineBean arg) throws Exception {
	    throw new UnsupportedOperationException("We presently serialize LineString as EncodedPolylineBean, and thus cannot deserialize them");
	}

	@Override
	public EncodedPolylineBean marshal(LineString arg) throws Exception {
		if (arg == null) {
			return null;
		}
		Coordinate[] lineCoords = arg.getCoordinates();
		List<Coordinate> coords = Arrays.asList(lineCoords);
		return PolylineEncoder.createEncodings(coords);
	}

	
}
