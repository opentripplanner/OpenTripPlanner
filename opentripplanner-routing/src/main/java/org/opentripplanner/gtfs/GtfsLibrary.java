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

package org.opentripplanner.gtfs;


import java.io.File;
import java.io.IOException;

import org.onebusaway.gtfs.impl.GtfsRelationalDaoImpl;
import org.onebusaway.gtfs.impl.calendar.CalendarServiceDataFactoryImpl;
import org.onebusaway.gtfs.impl.calendar.CalendarServiceImpl;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.calendar.CalendarServiceData;
import org.onebusaway.gtfs.serialization.GtfsReader;
import org.onebusaway.gtfs.services.GtfsRelationalDao;
import org.onebusaway.gtfs.services.calendar.CalendarService;
import org.opentripplanner.routing.core.TraverseMode;

public class GtfsLibrary {

    public static final char ID_SEPARATOR = '_';

    public static GtfsContext createContext(GtfsRelationalDao dao) {
        CalendarService calendarService = createCalendarService(dao);
        return createContext(dao,calendarService);
    }

    public static GtfsContext createContext(GtfsRelationalDao dao, CalendarService calendarService) {
        return new GtfsContextImpl(dao, calendarService);
    }

    public static GtfsContext readGtfs(File path) throws IOException {
        return readGtfs(path, null);
    }

    public static GtfsContext readGtfs(File path, String defaultAgencyId) throws IOException {

        GtfsRelationalDaoImpl dao = new GtfsRelationalDaoImpl();

        GtfsReader reader = new GtfsReader();
        reader.setInputLocation(path);
        reader.setEntityStore(dao);

        if (defaultAgencyId != null)
            reader.setDefaultAgencyId(defaultAgencyId);

        reader.run();

        CalendarService calendarService = createCalendarService(dao);

        return new GtfsContextImpl(dao, calendarService);
    }

    public static CalendarService createCalendarService(GtfsRelationalDao dao) {
        CalendarServiceData data = createCalendarServiceData(dao);
        CalendarServiceImpl service = new CalendarServiceImpl();
        service.setData(data);
        return service;
    }

    public static CalendarServiceData createCalendarServiceData(GtfsRelationalDao dao) {
        CalendarServiceDataFactoryImpl factory = new CalendarServiceDataFactoryImpl();
        factory.setGtfsDao(dao);
        CalendarServiceData data = factory.createData();
        return data;
    }

    public static AgencyAndId convertIdFromString(String value) {
        int index = value.indexOf(ID_SEPARATOR);
        if (index == -1) {
            throw new IllegalStateException("invalid agency-and-id: " + value);
        } else {
            return new AgencyAndId(value.substring(0, index), value.substring(index + 1));
        }
    }

    public static String convertIdToString(AgencyAndId aid) {
        return aid.getAgencyId() + ID_SEPARATOR + aid.getId();
    }

    public static String getRouteName(Route route) {
        if (route.getShortName() != null)
            return route.getShortName();
        return route.getLongName();
    }

    public static TraverseMode getTraverseMode(Route route) {
        switch (route.getType()) {
        case 0:
            return TraverseMode.TRAM;
        case 1:
            return TraverseMode.SUBWAY;
        case 2:
            return TraverseMode.RAIL;
        case 3:
            return TraverseMode.BUS;
        case 4:
            return TraverseMode.FERRY;
        case 5:
            return TraverseMode.CABLE_CAR;
        case 6:
            return TraverseMode.GONDOLA;
        case 7:
            return TraverseMode.FUNICULAR;
        default:
            throw new IllegalArgumentException("unknown gtfs route type " + route.getType());
        }
    }

    private static class GtfsContextImpl implements GtfsContext {

        private GtfsRelationalDao _dao;

        private CalendarService _calendar;
        
        public GtfsContextImpl(GtfsRelationalDao dao, CalendarService calendar) {
            _dao = dao;
            _calendar = calendar;
        }

        @Override
        public GtfsRelationalDao getDao() {
            return _dao;
        }

        @Override
        public CalendarService getCalendarService() {
            return _calendar;
        }
    }
}
