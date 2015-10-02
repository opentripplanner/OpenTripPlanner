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

        g.streetSpeedSource = new StreetSpeedSnapshotSource();
        g.streetSpeedSource.setSnapshot(new StreetSpeedSnapshot(speeds));

        // confirm that we get the correct speeds.
        // This also implicitly tests encoding/decoding

        OffsetDateTime odt = OffsetDateTime.of(2015, 6, 1, 8, 5, 0, 0, ZoneOffset.UTC);

        StreetSpeedSnapshot snap = g.streetSpeedSource.getSnapshot();

        double monday8am = snap.getSpeed(se, TraverseMode.CAR, odt.toInstant().toEpochMilli());
        // no data: should use average
        assertEquals(1.33, monday8am, 0.1);

        odt = odt.plusHours(1);

        double monday9am = snap.getSpeed(se, TraverseMode.CAR, odt.toInstant().toEpochMilli());
        assertEquals(6.1, monday9am, 0.1);

        odt = odt.plusHours(1);

        double monday10am =  snap.getSpeed(se, TraverseMode.CAR, odt.toInstant().toEpochMilli());
        assertEquals(33.3, monday10am, 0.1);

        se.wayId = 102;
        double wrongStreet = snap.getSpeed(se, TraverseMode.CAR, odt.toInstant().toEpochMilli());
        assertTrue(Double.isNaN(wrongStreet));
    }

    @Test
    public void testConcurrency () {
        Graph g = new Graph();

        StreetSpeedSnapshotSource ssss = new StreetSpeedSnapshotSource();

        OsmVertex v1 = new OsmVertex(g, "v1", 0, 0, 5l);
        OsmVertex v2 = new OsmVertex(g, "v2", 0, 0.01, 6l);
        StreetEdge se = new StreetEdge(v1, v2, null, "test", 1000, StreetTraversalPermission.CAR, false);
        se.wayId = 10;

        Map<Segment, SegmentSpeedSample> ss2 = Maps.newHashMap();
        Segment seg = new Segment(10l, 5l, 6l);
        ss2.put(seg, getSpeedSample());
        StreetSpeedSnapshot ssOrig = new StreetSpeedSnapshot(ss2);
        ssss.setSnapshot(ssOrig);
        StreetSpeedSnapshot snap = ssss.getSnapshot();
        assertEquals(ssOrig, snap);

        // should be match
        assertFalse(Double.isNaN(snap.getSpeed(se, TraverseMode.CAR, System.currentTimeMillis())));

        // should not have changed
        assertEquals(snap, ssss.getSnapshot());

        Map<Segment, SegmentSpeedSample> ss1 = Maps.newHashMap();
        seg = new Segment(10l, 4l, 6l);
        ss1.put(seg, getSpeedSample());
        StreetSpeedSnapshot ssNew = new StreetSpeedSnapshot(ss1);
        ssss.setSnapshot(ssNew);

        snap = ssss.getSnapshot();
        assertEquals(ssNew, snap);

        // should be no match; the segment in the index does not match the street edge
        assertTrue(Double.isNaN(snap.getSpeed(se, TraverseMode.CAR, System.currentTimeMillis())));
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