package org.opentripplanner.api.model.transit;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;

import org.onebusaway.gtfs.model.ServiceCalendar;
import org.onebusaway.gtfs.model.ServiceCalendarDate;

public class ServiceCalendarData {
    @XmlElement
    public List<ServiceCalendar> calendars;

    @XmlElement
    public List<ServiceCalendarDate> calendarDates;
}
