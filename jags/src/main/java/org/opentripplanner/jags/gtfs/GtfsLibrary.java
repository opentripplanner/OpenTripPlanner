package org.opentripplanner.jags.gtfs;

import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static java.lang.Math.toRadians;

import org.onebusaway.gtfs.impl.GtfsRelationalDaoImpl;
import org.onebusaway.gtfs.impl.calendar.CalendarServiceDataFactoryImpl;
import org.onebusaway.gtfs.impl.calendar.CalendarServiceImpl;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.serialization.GtfsReader;
import org.onebusaway.gtfs.services.GtfsRelationalDao;
import org.onebusaway.gtfs.services.calendar.CalendarService;
import org.onebusaway.gtfs.services.calendar.CalendarServiceData;

import org.opentripplanner.jags.core.TransportationMode;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

public class GtfsLibrary {

  private static final double RADIUS_OF_EARTH_IN_KM = 6371.01;

  public static GtfsContext readGtfs(File path) throws IOException {

    GtfsRelationalDaoImpl dao = new GtfsRelationalDaoImpl();

    GtfsReader reader = new GtfsReader();
    reader.setInputLocation(path);
    reader.setEntityStore(dao);
    reader.run();

    CalendarService calendarService = createCalendarService(dao);

    return new GtfsContextImpl(dao,calendarService);
  }

  public static CalendarService createCalendarService(GtfsRelationalDao dao) {

    CalendarServiceDataFactoryImpl factory = new CalendarServiceDataFactoryImpl();
    factory.setGtfsDao(dao);
    CalendarServiceData data = factory.createServiceCalendarData();

    CalendarServiceImpl service = new CalendarServiceImpl();
    service.setServiceCalendarData(data);
    return service;
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
        throw new IllegalArgumentException("unknown gtfs route type "
            + route.getType());
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

  public static final double distance(double lat1, double lon1, double lat2,
      double lon2) {
    return distance(lat1, lon1, lat2, lon2, RADIUS_OF_EARTH_IN_KM * 1000);
  }

  private static final double distance(double lat1, double lon1, double lat2,
      double lon2, double radius) {

    // http://en.wikipedia.org/wiki/Great-circle_distance
    lat1 = toRadians(lat1); // Theta-s
    lon1 = toRadians(lon1); // Lambda-s
    lat2 = toRadians(lat2); // Theta-f
    lon2 = toRadians(lon2); // Lambda-f

    double deltaLon = lon2 - lon1;

    double y = sqrt(p2(cos(lat2) * sin(deltaLon))
        + p2(cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(deltaLon)));
    double x = sin(lat1) * sin(lat2) + cos(lat1) * cos(lat2) * cos(deltaLon);

    return radius * atan2(y, x);
  }

  private static final double p2(double a) {
    return a * a;
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
