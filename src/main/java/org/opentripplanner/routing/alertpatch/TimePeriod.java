package org.opentripplanner.routing.alertpatch;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Represents a period of time, in terms of seconds in [start, end)
 * @author novalis
 *
 */
@XmlType
public class TimePeriod {
    public TimePeriod(long start, long end) {
        this.startTime = start;
        this.endTime = end;
    }

    public TimePeriod() {
    }

    @XmlAttribute
    @JsonSerialize
    public long startTime;

    @XmlAttribute
    @JsonSerialize
    public long endTime;

    public boolean equals(Object o) {
        if (!(o instanceof TimePeriod)) {
            return false;
        }
        TimePeriod other = (TimePeriod) o;
        return other.startTime == startTime && other.endTime == endTime;
    }

    public int hashCode() {
        return (int) ((startTime & 0x7fff) + (endTime & 0x7fff));
    }
}
