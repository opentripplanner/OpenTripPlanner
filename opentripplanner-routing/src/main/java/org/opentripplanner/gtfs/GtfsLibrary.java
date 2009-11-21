package org.opentripplanner.gtfs;


import org.onebusaway.gtfs.impl.GtfsRelationalDaoImpl;
import org.onebusaway.gtfs.impl.calendar.CalendarServiceDataFactoryImpl;
import org.onebusaway.gtfs.impl.calendar.CalendarServiceImpl;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.serialization.GtfsReader;
import org.onebusaway.gtfs.services.GtfsRelationalDao;
import org.onebusaway.gtfs.services.calendar.CalendarService;
import org.onebusaway.gtfs.services.calendar.CalendarServiceData;

import org.opentripplanner.routing.core.TransportationMode;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

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

        CalendarServiceDataFactoryImpl factory = new CalendarServiceDataFactoryImpl();
        factory.setGtfsDao(dao);
        CalendarServiceData data = factory.createServiceCalendarData();

        CalendarServiceImpl service = new CalendarServiceImpl();
        service.setServiceCalendarData(data);
        return service;
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

    public static TransportationMode getTransportationMode(Route route) {
        switch (route.getType()) {
        case 0:
            return TransportationMode.TRAM;
        case 1:
            return TransportationMode.SUBWAY;
        case 2:
            return TransportationMode.RAIL;
        case 3:
            return TransportationMode.BUS;
        case 4:
            return TransportationMode.FERRY;
        case 5:
            return TransportationMode.CABLE_CAR;
        case 6:
            return TransportationMode.GONDOLA;
        case 7:
            return TransportationMode.FUNICULAR;
        default:
            throw new IllegalArgumentException("unknown gtfs route type " + route.getType());
        }
    }

    public static long getTimeAsDay(long time) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(time);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
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
