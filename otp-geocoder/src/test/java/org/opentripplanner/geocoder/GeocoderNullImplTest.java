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

package org.opentripplanner.geocoder;

import static org.junit.Assert.*;

import org.junit.Test;

public class GeocoderNullImplTest {

    @Test
    public void testGeocode() {
        Geocoder nullGeocoder = new GeocoderNullImpl();
        GeocoderResults result = nullGeocoder.geocode("121 elm street", null);
        assertEquals("stub response", GeocoderNullImpl.ERROR_MSG, result.getError());
    }
}