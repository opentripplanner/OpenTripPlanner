package org.opentripplanner.api.adapters;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.ServiceCalendarDate;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@XmlRootElement(name = "CalendarDate")
public class ServiceCalendarDateType {

    public ServiceCalendarDateType(FeedScopedId serviceId, long date, int exceptionType) {
        this.serviceId = serviceId;
        this.date = date;
        this.exceptionType = exceptionType;
        switch (this.exceptionType) {
        case 1:
            this.exception = "Remove";
            break;
        case 2:
            this.exception = "Add";
            break;
        default:
            this.exception = "";
        }
    }

    public ServiceCalendarDateType(ServiceCalendarDate arg) {
        this.serviceId = arg.getServiceId();
        this.date = arg.getDate().getAsDate().getTime();
        this.exceptionType = arg.getExceptionType();
        switch (this.exceptionType) {
        case 1:
            this.exception = "Remove";
            break;
        case 2:
            this.exception = "Add";
            break;
        default:
            this.exception = "";
        }
    }

    public ServiceCalendarDateType() {
    }

    @XmlJavaTypeAdapter(AgencyAndIdAdapter.class)
    @JsonSerialize
    FeedScopedId serviceId;

    @XmlAttribute
    @JsonSerialize
    Long date;

    @XmlAttribute
    @JsonSerialize
    Integer exceptionType;

    @XmlAttribute
    @JsonSerialize
    String exception;

}