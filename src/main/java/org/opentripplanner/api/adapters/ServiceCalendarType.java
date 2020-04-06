package org.opentripplanner.api.adapters;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.opentripplanner.api.mapping.FeedScopedIdMapper;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.calendar.ServiceCalendar;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "Calendar")
public class ServiceCalendarType {

    public ServiceCalendarType(FeedScopedId serviceId, int monday, int tuesday, int wednesday,
                               int thursday, int friday, int saturday, int sunday, long startDate, long endDate) {
        this.serviceId = FeedScopedIdMapper.mapToApi(serviceId);
        this.monday = monday;
        this.tuesday = tuesday;
        this.wednesday = wednesday;
        this.thursday = thursday;
        this.friday = friday;
        this.saturday = saturday;
        this.sunday = sunday;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public ServiceCalendarType(ServiceCalendar arg) {
        this.serviceId = FeedScopedIdMapper.mapToApi(arg.getServiceId());
        this.monday = arg.getMonday();
        this.tuesday = arg.getTuesday();
        this.wednesday = arg.getWednesday();
        this.thursday = arg.getThursday();
        this.friday = arg.getFriday();
        this.saturday = arg.getSaturday();
        this.sunday = arg.getSunday();
        this.startDate = arg.getPeriod().getStart().getAsDate().getTime();
        this.endDate = arg.getPeriod().getEnd().getAsDate().getTime();
    }

    public ServiceCalendarType() {
    }

    @JsonSerialize
    public String serviceId;

    @XmlAttribute
    @JsonSerialize
    public Integer monday;

    @XmlAttribute
    @JsonSerialize
    public Integer tuesday;

    @XmlAttribute
    @JsonSerialize
    public Integer wednesday;

    @XmlAttribute
    @JsonSerialize
    public Integer thursday;

    @XmlAttribute
    @JsonSerialize
    public Integer friday;

    @XmlAttribute
    @JsonSerialize
    public Integer saturday;

    @XmlAttribute
    @JsonSerialize
    public Integer sunday;

    @XmlAttribute
    @JsonSerialize
    public Long startDate;

    @XmlAttribute
    @JsonSerialize
    public Long endDate;

}