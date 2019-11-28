package org.opentripplanner.geocoder.bano;

import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.geocoder.GeocoderResult;
import org.opentripplanner.geocoder.GeocoderResults;

import java.io.IOException;
import java.net.URL;
import java.net.UnknownHostException;

public class BanoGeocoderTest {

    /**
     * NOTE! THIS TEST RELAY ON AN ON-LINE EXTERNAL API (Bano Geocoder) TO BE UP AN RUNNING, WHICH
     * MAY NOT BE THE CASE. HENCE THE '@Ignore'.
     * <p>
     * The test is kept here to be able to run it manually and to let downstream forks enable it,
     * but please do not include this in the main OTP GitHub, when failing it interrupts everyone's
     * work. It have failed twice in 2019, each time taking several hours to merge, review and
     * verify various forks and branches. Just at Entur we have more than 10 branches failing.
     */
    @Test
    @Ignore
    public void testOnLine() throws IOException {
        assumeConnectedToInternet();

        BanoGeocoder banoGeocoder = new BanoGeocoder();
        // The Presidential palace of the French Republic is not supposed to move often
        Envelope bbox = new Envelope();
        bbox.expandToInclude(2.25, 48.8);
        bbox.expandToInclude(2.35, 48.9);
        GeocoderResults results = banoGeocoder.geocode("55 Rue du Faubourg Saint-Honoré", bbox);

        assert (results.getResults().size() >= 1);

        boolean found = false;
        for (GeocoderResult result : results.getResults()) {
            if (result.getDescription().startsWith("55 Rue du Faubourg")) {
                double dist = SphericalDistanceLibrary.distance(result.getLat(),
                        result.getLng(), 48.870637, 2.316939);
                assert (dist < 100);
                found = true;
            }
        }
        assert (found);

    }

    private static void assumeConnectedToInternet() throws IOException {
        try {
            new URL("http://www.google.com").openConnection().connect();
        } catch (UnknownHostException e) {
            Assume.assumeTrue("Skips tests if not on internet.", false);
        }
    }
}
