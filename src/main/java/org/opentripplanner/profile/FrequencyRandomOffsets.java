package org.opentripplanner.profile;

import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.apache.commons.math3.random.MersenneTwister;

/**
 * Stores random offsets for frequency trips.
 * This is not in RaptorWorkerData as RaptorWorkerData may be shared between threads.
 */
public class FrequencyRandomOffsets {
    public final TIntObjectMap<int[]> offsets = new TIntObjectHashMap<>();
    public final RaptorWorkerData data;

    /** The mersenne twister is a higher quality random number generator than the one included with Java */
    private MersenneTwister mt = new MersenneTwister();

    public FrequencyRandomOffsets(RaptorWorkerData data) {
        this.data = data;

        if (!data.hasFrequencies)
            return;

        data.timetablesForPattern.stream().filter(tt -> tt.hasFrequencyTrips())
                .forEach(tt -> {
                    offsets.put(tt.dataIndex, new int[tt.getFrequencyTripCount()]);
                });
    }

    public void randomize () {
        for (TIntObjectIterator<int[]> it = offsets.iterator(); it.hasNext();) {
            it.advance();
            int[] newVal = new int[it.value().length];

            RaptorWorkerTimetable tt = data.timetablesForPattern.get(it.key());

            for (int i = 0; i < newVal.length; i++) {
                newVal[i] = mt.nextInt(tt.headwaySecs[i]);
            }

            it.setValue(newVal);
        }

    }
}
