package org.opentripplanner.mmri;

import org.opentripplanner.GtfsTest;

/** Common base class for all the MMRI tests (see package-info). */
abstract class MmriTest extends GtfsTest {

    public boolean isLongDistance() { return true; }

}
