package org.opentripplanner.traffic;

import com.google.common.collect.Maps;
import junit.framework.TestCase;
import org.junit.Test;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.OsmVertex;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

/** Test that street speed sources get used */
public class StreetSpeedSourceTest extends TestCase {
    @Test
    public void testMatching () {
        Graph g = new Graph();
        OsmVertex v1 = new OsmVertex(g, "v1", 0, 0, 5l);
        OsmVertex v2 = new OsmVertex(g, "v2", 0, 0.01, 6l);
        StreetEdge se = new StreetEdge(v1, v2, null, "test", 1000, StreetTraversalPermission.CAR, false);
        se.wayId = 10;

        // create a speed sample
        SegmentSpeedSample s = getSpeedSample();

        Map<Segment, SegmentSpeedSample> speeds = Maps.newHashMap();
        Segment seg = new Segment(10l, 5l, 6l);
        speeds.put(seg, s);

        g.streetSpeedSource = new StreetSpeedSource();
        g.streetSpeedSource.setSamples(speeds);

        // confirm that we get the correct speeds.
        // This also implicitly tests encoding/decoding

        OffsetDateTime odt = OffsetDateTime.of(2015, 6, 1, 8, 5, 0, 0, ZoneOffset.UTC);

        double monday8am = g.streetSpeedSource.getSpeed(se, TraverseMode.CAR, odt.toInstant().toEpochMilli());
        // no data: should use average
        assertEquals(1.33, monday8am, 0.1);

        odt = odt.plusHours(1);

        double monday9am = g.streetSpeedSource.getSpeed(se, TraverseMode.CAR, odt.toInstant().toEpochMilli());
        assertEquals(6.1, monday9am, 0.1);

        odt = odt.plusHours(1);

        double monday10am =  g.streetSpeedSource.getSpeed(se, TraverseMode.CAR, odt.toInstant().toEpochMilli());
        assertEquals(33.3, monday10am, 0.1);

        se.wayId = 102;
        double wrongStreet = g.streetSpeedSource.getSpeed(se, TraverseMode.CAR, odt.toInstant().toEpochMilli());
        assertTrue(Double.isNaN(wrongStreet));
    }

    /** Make a speed sample */
    private SegmentSpeedSample getSpeedSample() {
        double[] hourBins = new double[7 * 24];
        for (int i = 0; i < hourBins.length; i++) {
            if (i == 8)
                hourBins[i] = Double.NaN;
            else if (i == 9)
                // ~20km/h
                hourBins[i] = 6.1;
            else
                // ~120 km/h
                hourBins[i] = 33.3;

        }

        // ~4km/h
        double avg = 1.33;

        return new SegmentSpeedSample(avg, hourBins);
    }
}