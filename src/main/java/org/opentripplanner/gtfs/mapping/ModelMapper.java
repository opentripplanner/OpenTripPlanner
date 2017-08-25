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

package org.opentripplanner.gtfs.mapping;

import org.onebusaway2.gtfs.impl.GtfsRelationalDaoImpl;
import org.onebusaway2.gtfs.services.GtfsMutableRelationalDao;

public class ModelMapper {
    private final AgencyMapper agencyMapper = new AgencyMapper();
    private final StopMapper stopMapper = new StopMapper();
    private final FareAttributeMapper fareAttributeMapper = new FareAttributeMapper();
    private final ServiceCalendarDateMapper serviceCalendarDateMapper = new ServiceCalendarDateMapper();
    private final FeedInfoMapper feedInfoMapper = new FeedInfoMapper();
    private final ShapePointMapper shapePointMapper = new ShapePointMapper();
    private final ServiceCalendarMapper serviceCalendarMapper = new ServiceCalendarMapper();

    private final PathwayMapper pathwayMapper = new PathwayMapper(stopMapper);
    private final RouteMapper routeMapper = new RouteMapper(agencyMapper);
    private final TripMapper tripMapper = new TripMapper(routeMapper);
    private final StopTimeMapper stopTimeMapper = new StopTimeMapper(stopMapper, tripMapper);

    private final FrequencyMapper frequencyMapper = new FrequencyMapper(tripMapper);
    private final TransferMapper transferMapper = new TransferMapper(routeMapper, stopMapper, tripMapper);

    private final FareRuleMapper fareRuleMapper = new FareRuleMapper(routeMapper, fareAttributeMapper);

    public static GtfsMutableRelationalDao mapDao(
            org.onebusaway.gtfs.services.GtfsMutableRelationalDao data
    ) {
        return new ModelMapper().map(data);
    }

    private GtfsMutableRelationalDao map(
            org.onebusaway.gtfs.services.GtfsMutableRelationalDao data
    ) {

        // TODO TGR - Remove this if it works
        if(
                ((org.onebusaway.gtfs.impl.GtfsRelationalDaoImpl)data).isPackShapePoints() ||
        ((org.onebusaway.gtfs.impl.GtfsRelationalDaoImpl)data).isPackStopTimes()
                ) {
            System.err.println("!!! ------------------------------------------------------");
            System.err.println("!!! -->  Data is packed, not expected: " + data);
            System.err.println("!!! ------------------------------------------------------");
        }

        return new GtfsRelationalDaoImpl(
                agencyMapper.map(data.getAllAgencies()),
                serviceCalendarDateMapper.map(data.getAllCalendarDates()),
                serviceCalendarMapper.map(data.getAllCalendars()),
                fareAttributeMapper.map(data.getAllFareAttributes()),
                fareRuleMapper.map(data.getAllFareRules()),
                feedInfoMapper.map(data.getAllFeedInfos()),
                frequencyMapper.map(data.getAllFrequencies()),
                pathwayMapper.map(data.getAllPathways()),
                routeMapper.map(data.getAllRoutes()),
                shapePointMapper.map(data.getAllShapePoints()),
                stopMapper.map(data.getAllStops()),
                stopTimeMapper.map(data.getAllStopTimes()),
                transferMapper.map(data.getAllTransfers()),
                tripMapper.map(data.getAllTrips())
        );
    }
}
