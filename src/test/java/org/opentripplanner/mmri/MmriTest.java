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

package org.opentripplanner.mmri;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.GtfsTest;
import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.api.model.Leg;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.api.resource.PlanGenerator;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.graph_builder.impl.GtfsGraphBuilderImpl;
import org.opentripplanner.graph_builder.model.GtfsBundle;
import org.opentripplanner.routing.algorithm.GenericAStar;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.AlertPatchServiceImpl;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;
import org.opentripplanner.routing.impl.LongDistancePathService;
import org.opentripplanner.updater.alerts.AlertsUpdateHandler;
import org.opentripplanner.updater.stoptime.TimetableSnapshotSource;

import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;

/** Common base class for all the MMRI tests (see package-info). */
abstract class MmriTest extends GtfsTest {

    public boolean isLongDistance() { return true; }

}
