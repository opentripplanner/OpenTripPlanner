package org.opentripplanner.geocoder.bano;

import org.junit.Test;
import org.junit.Ignore;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.geocoder.GeocoderResult;
import org.opentripplanner.geocoder.GeocoderResults;

import org.locationtech.jts.geom.Envelope;

public class BanoGeocoderTest {

    /**
     * NOTE! THIS TEST RELAY ON AN ON-LINE EXTERNAL API (Bano Geocoder) TO BE UP AN RUNNING, WHICH
     * MAY NOT BE THE CASE. HENCE THE '@Ignore'.
     * <p>
     * TODO -- This unit-test rely on an on-line API to be up and running, which may not be the case
     * if a network connection is not active or the server is down.
     */
    @Test
    @Ignore
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
            if (result.getDescription().contains("55 Rue du Faubourg")) {
                double dist = SphericalDistanceLibrary.distance(result.getLat(),
                        result.getLng(), 48.870637, 2.316939);
                assert (dist < 100);
                found = true;
            }
        }
        assert (found);

    }

}
