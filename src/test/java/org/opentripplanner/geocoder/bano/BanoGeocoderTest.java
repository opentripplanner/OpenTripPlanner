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

package org.opentripplanner.geocoder.bano;

import org.junit.Test;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.geocoder.GeocoderResult;
import org.opentripplanner.geocoder.GeocoderResults;

import com.vividsolutions.jts.geom.Envelope;

public class BanoGeocoderTest {

    /**
     * TODO -- This unit-test rely on an on-line API to be up and running, which may not be the case
     * if a network connection is not active or the server is down.
     */
    @Test
    public void testOnLine() throws Exception {

        BanoGeocoder banoGeocoder = new BanoGeocoder();
        // The Presidential palace of the French Republic is not supposed to move often
        Envelope bbox = new Envelope();
        bbox.expandToInclude(2.25, 48.8);
        bbox.expandToInclude(2.35, 48.9);
        GeocoderResults results = banoGeocoder.geocode("55 Rue du Faubourg Saint-Honoré", bbox);

        assert (results.getResults().size() >= 1);

        boolean found = false;
        for (GeocoderResult result : results.getResults()) {
            if ("55 Rue du Faubourg Saint-Honoré 75008 Paris".equals(result.getDescription())) {
                double dist = SphericalDistanceLibrary.distance(result.getLat(),
                        result.getLng(), 48.870637, 2.316939);
                assert (dist < 100);
                found = true;
            }
        }
        assert (found);

    }

}
