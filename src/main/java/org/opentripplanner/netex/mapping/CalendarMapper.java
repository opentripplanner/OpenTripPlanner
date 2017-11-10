/*
 This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package org.opentripplanner.netex.mapping;

import org.opentripplanner.graph_builder.model.NetexDao;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.ServiceCalendarDate;
import org.opentripplanner.model.calendar.ServiceDate;
import org.rutebanken.netex.model.DayOfWeekEnumeration;
import org.rutebanken.netex.model.DayType;
import org.rutebanken.netex.model.DayTypeAssignment;
import org.rutebanken.netex.model.OperatingPeriod;
import org.rutebanken.netex.model.PropertyOfDay;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;


// TODO TGR - Add Unit tests
public class CalendarMapper {

    public static Collection<ServiceCalendarDate> mapToCalendarDates(FeedScopedId serviceId, NetexDao netexDao) {
        Collection<ServiceCalendarDate> serviceCalendarDates = new ArrayList<>();
        Collection<ServiceCalendarDate> serviceCalendarDatesRemove = new HashSet<>();
        String[] dayTypeIds = serviceId.getId().split("\\+");

        for(String dayTypeId : dayTypeIds) {
            List<DayTypeAssignment> dayTypeAssignmentList = netexDao.getDayTypeAssignment().get(dayTypeId);


            for (DayTypeAssignment dayTypeAssignment : dayTypeAssignmentList) {
                Boolean isAvailable = netexDao.getDayTypeAvailable().get(dayTypeAssignment.getId());

                // Add or remove single days
                if (dayTypeAssignment.getDate() != null) {
                    LocalDateTime date = dayTypeAssignment.getDate();
                    ServiceCalendarDate serviceCalendarDate = mapServiceCalendarDate(date, serviceId, 1);
                    if (isAvailable) {
                        serviceCalendarDates.add(serviceCalendarDate);
                    } else {
                        serviceCalendarDatesRemove.add(serviceCalendarDate);
                    }
                }
                // Add or remove periods
                else if (dayTypeAssignment.getOperatingPeriodRef() != null &&
                        netexDao.getOperatingPeriodById().containsKey(dayTypeAssignment.getOperatingPeriodRef().getRef())) {

                    OperatingPeriod operatingPeriod = netexDao.getOperatingPeriodById().get(dayTypeAssignment.getOperatingPeriodRef().getRef());
                    LocalDateTime fromDate = operatingPeriod.getFromDate();
                    LocalDateTime toDate = operatingPeriod.getToDate();

                    List<DayOfWeekEnumeration> daysOfWeek = new ArrayList<>();
                    DayType dayType = netexDao.getDayTypeById().get(dayTypeId);
                    if (dayType.getProperties() != null) {
                        List<PropertyOfDay> propertyOfDays = dayType.getProperties().getPropertyOfDay();
                        for (PropertyOfDay property : propertyOfDays) {
                            daysOfWeek.addAll(property.getDaysOfWeek());
                        }
                    }

                    for (LocalDateTime date = fromDate; date.isBefore(toDate); date = date.plusDays(1)) {
                        ServiceCalendarDate serviceCalendarDate = mapServiceCalendarDate(date, serviceId, 1);

                        if (daysOfWeek.contains(DayOfWeekEnumeration.EVERYDAY)) {
                            serviceCalendarDates.add(serviceCalendarDate);
                        } else {
                            switch (date.getDayOfWeek()) {
                                case MONDAY:
                                    if (daysOfWeek.contains(DayOfWeekEnumeration.WEEKDAYS) || daysOfWeek.contains(DayOfWeekEnumeration.MONDAY)) {
                                        if (isAvailable) {
                                            serviceCalendarDates.add(serviceCalendarDate);
                                        } else {
                                            serviceCalendarDatesRemove.add(serviceCalendarDate);
                                        }
                                    }
                                    break;
                                case TUESDAY:
                                    if (daysOfWeek.contains(DayOfWeekEnumeration.WEEKDAYS) || daysOfWeek.contains(DayOfWeekEnumeration.TUESDAY)) {
                                        if (isAvailable) {
                                            serviceCalendarDates.add(serviceCalendarDate);
                                        } else {
                                            serviceCalendarDatesRemove.add(serviceCalendarDate);
                                        }
                                    }
                                    break;
                                case WEDNESDAY:
                                    if (daysOfWeek.contains(DayOfWeekEnumeration.WEEKDAYS) || daysOfWeek.contains(DayOfWeekEnumeration.WEDNESDAY)) {
                                        if (isAvailable) {
                                            serviceCalendarDates.add(serviceCalendarDate);
                                        } else {
                                            serviceCalendarDatesRemove.add(serviceCalendarDate);
                                        }
                                    }
                                    break;
                                case THURSDAY:
                                    if (daysOfWeek.contains(DayOfWeekEnumeration.WEEKDAYS) || daysOfWeek.contains(DayOfWeekEnumeration.THURSDAY)) {
                                        if (isAvailable) {
                                            serviceCalendarDates.add(serviceCalendarDate);
                                        } else {
                                            serviceCalendarDatesRemove.add(serviceCalendarDate);
                                        }
                                    }
                                    break;
                                case FRIDAY:
                                    if (daysOfWeek.contains(DayOfWeekEnumeration.WEEKDAYS) || daysOfWeek.contains(DayOfWeekEnumeration.FRIDAY)) {
                                        if (isAvailable) {
                                            serviceCalendarDates.add(serviceCalendarDate);
                                        } else {
                                            serviceCalendarDatesRemove.add(serviceCalendarDate);
                                        }
                                    }
                                    break;
                                case SATURDAY:
                                    if (daysOfWeek.contains(DayOfWeekEnumeration.WEEKEND) || daysOfWeek.contains(DayOfWeekEnumeration.SATURDAY)) {
                                        if (isAvailable) {
                                            serviceCalendarDates.add(serviceCalendarDate);
                                        } else {
                                            serviceCalendarDatesRemove.add(serviceCalendarDate);
                                        }
                                    }
                                    break;
                                case SUNDAY:
                                    if (daysOfWeek.contains(DayOfWeekEnumeration.WEEKEND) || daysOfWeek.contains(DayOfWeekEnumeration.SUNDAY)) {
                                        if (isAvailable) {
                                            serviceCalendarDates.add(serviceCalendarDate);
                                        } else {
                                            serviceCalendarDatesRemove.add(serviceCalendarDate);
                                        }
                                    }
                                    break;
                            }
                        }

                    }
                }
            }
        }

        serviceCalendarDates.removeAll(serviceCalendarDatesRemove);

        return serviceCalendarDates;
    }

    private static ServiceCalendarDate mapServiceCalendarDate(LocalDateTime date, FeedScopedId serviceId, Integer exceptionType) {
        ServiceCalendarDate serviceCalendarDate = new ServiceCalendarDate();
        serviceCalendarDate.setServiceId(serviceId);
        serviceCalendarDate.setDate(new ServiceDate(date.getYear(), date.getMonthValue(), date.getDayOfMonth()));
        serviceCalendarDate.setExceptionType(exceptionType);
        return serviceCalendarDate;
    }
}