/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (props, at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.api.resource;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

/**
 * Holds information to be included in the REST Response for debugging and profiling purposes.
 *
 * startedCalculating is called in the routingContext constructor.
 * finishedCalculating and finishedRendering are all called in PlanGenerator.generate().
 * finishedPrecalculating and foundPaths are called in the SPTService implementations.
 */
@XmlRootElement
public class DebugOutput {

    private static final Logger LOG = LoggerFactory.getLogger(DebugOutput.class);

    /* Only public fields are serialized by JAX-RS, make interal ones private? */
    private long startedCalculating;
    private long finishedPrecalculating;
    private List<Long> foundPaths = Lists.newArrayList();
    private long finishedCalculating;
    private long finishedRendering;

    /* Results, public to cause JAX-RS serialization */
    public long precalculationTime;
    public long pathCalculationTime;
    public List<Long> pathTimes = Lists.newArrayList();
    public long renderingTime;
    public long totalTime;
    public boolean timedOut;

    /**
     * Record the time when we first began calculating a path for this request
     * (before any heuristic pre-calculation). Note that timings will not
     * include network and server request queue overhead, which is what we want.
     * finishedPrecalculating is also set because some heuristics will not mark any precalculation
     * step, and path times are measured from when precalculation ends.
     */
    public void startedCalculating() {
        startedCalculating = finishedPrecalculating = System.currentTimeMillis();
    }

    /** Record the time when we finished heuristic pre-calculation. */
    public void finishedPrecalculating() {
        finishedPrecalculating = System.currentTimeMillis();
    }

    /** Record the time when a path was found. */
    public void foundPath() {
        foundPaths.add(System.currentTimeMillis());
    }

    /** Record the time when we finished calculating paths for this request. */
    public void finishedCalculating() {
        finishedCalculating = System.currentTimeMillis();
    }

    /** Record the time when we finished converting paths into itineraries. */
    public void finishedRendering() {
        finishedRendering = System.currentTimeMillis();
        computeSummary();
    }

    /** Summarize and calculate elapsed times. */
    private void computeSummary() {
        precalculationTime = finishedPrecalculating - startedCalculating;
        pathCalculationTime = finishedCalculating - finishedPrecalculating;
        long last_t = finishedPrecalculating;
        for (long t : foundPaths) {
            pathTimes.add(t - last_t);
            last_t = t;
        }
        LOG.debug("times to find each path: {}", pathTimes);
        renderingTime = finishedRendering - finishedCalculating;
        totalTime = finishedRendering - startedCalculating;
    }
}
