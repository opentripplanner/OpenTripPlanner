package org.opentripplanner.updater.stoptime;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.opentripplanner.routing.graph.Graph;

import com.google.transit.realtime.GtfsRealtime.TripUpdate;

public class TripUpdateGraphWriterRunnableTest {
    @Test
    public void testTripUpdateGraphWriterRunnable() {
        final boolean fullDataset = false;
        final String agencyId = "Agency ID";
        final List<TripUpdate> updates =
                Collections.singletonList(TripUpdate.newBuilder().buildPartial());
        final TripUpdateGraphWriterRunnable tripUpdateGraphWriterRunnable =
                new TripUpdateGraphWriterRunnable(fullDataset, updates, agencyId);

        Graph graph = mock(Graph.class);
        TimetableSnapshotSource timetableSnapshotSource = mock(TimetableSnapshotSource.class);

        graph.timetableSnapshotSource = timetableSnapshotSource;

        tripUpdateGraphWriterRunnable.run(graph);

        verify(timetableSnapshotSource).applyTripUpdates(graph, fullDataset, updates, agencyId);
    }
}
