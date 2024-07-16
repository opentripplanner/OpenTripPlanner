package org.opentripplanner.updater.spi;

import org.opentripplanner.ext.siri.SiriTimetableSnapshotSource;
import org.opentripplanner.updater.trip.TimetableSnapshotSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Flush the timetable snapshot buffer by committing pending changes.
 */
public class TimetableSnapshotFlush implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(TimetableSnapshotFlush.class);

  private final SiriTimetableSnapshotSource siriTimetableSnapshotSource;
  private final TimetableSnapshotSource gtfsTimetableSnapshotSource;

  public TimetableSnapshotFlush(
    SiriTimetableSnapshotSource siriTimetableSnapshotSource,
    TimetableSnapshotSource gtfsTimetableSnapshotSource
  ) {
    this.siriTimetableSnapshotSource = siriTimetableSnapshotSource;
    this.gtfsTimetableSnapshotSource = gtfsTimetableSnapshotSource;
  }

  @Override
  public void run() {
    LOG.debug("Flushing timetable snapshot buffer");
    if (siriTimetableSnapshotSource != null) {
      siriTimetableSnapshotSource.flushBuffer();
    }
    if (gtfsTimetableSnapshotSource != null) {
      gtfsTimetableSnapshotSource.flushBuffer();
    }
    LOG.debug("Flushed timetable snapshot buffer");
  }
}
