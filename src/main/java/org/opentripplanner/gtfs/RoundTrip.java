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

import com.csvreader.CsvWriter;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import org.opentripplanner.gtfs.format.Feed;
import org.opentripplanner.gtfs.model.Agency;
import org.opentripplanner.gtfs.model.CalendarDate;
import org.opentripplanner.gtfs.model.FeedInfo;
import org.opentripplanner.gtfs.model.Route;
import org.opentripplanner.gtfs.model.Shape;
import org.opentripplanner.gtfs.model.Stop;
import org.opentripplanner.gtfs.model.StopTime;
import org.opentripplanner.gtfs.model.Transfer;
import org.opentripplanner.gtfs.model.Trip;

import java.nio.charset.Charset;
import java.util.Map;

/**
 * This RoundTrip class is tailored to the OVapi GTFS feed (at http://gtfs.ovapi.nl/new/gtfs-nl.zip)
 */
public class RoundTrip {
    final static private Charset UTF8 = Charset.forName("UTF-8");

    public static void main (String[] args) {
        if (args.length < 1) {
            System.err.println("Please specify a GTFS feed input file for parsing and extraction.");
            System.exit(1);
        }

        try (Feed feed = new Feed(args[0])) {
            final long time = System.nanoTime();
            Iterable<Map<String, String>> agency = feed.get("agency.txt");
            if (agency != null) {
                CsvWriter csvWriter = new CsvWriter("agency.txt", ',', UTF8);
                Iterable<Agency> iterable = Iterables.transform(agency,
                        new Function<Map<String, String>, Agency>() {
                    @Override
                    public Agency apply(Map<String, String> row) {
                        return null; //new Agency(row);
                    }
                });
                try {
                    csvWriter.writeRecord(new String[]{"agency_id", "agency_name", "agency_url",
                            "agency_timezone", "agency_phone"});
                    for (Agency cardinal_mistake : iterable) {
                        final String fields[] = new String[5];
                        fields[0] = cardinal_mistake.agency_id;
                        fields[1] = cardinal_mistake.agency_name;
                        fields[2] = cardinal_mistake.agency_url;
                        fields[3] = cardinal_mistake.agency_timezone;
                        fields[4] = cardinal_mistake.agency_phone;
                        csvWriter.writeRecord(fields);
                    }
                } catch (Exception e) {
                    e.printStackTrace();

                    System.exit(2);
                } finally {
                    csvWriter.close();
                }
            } else {
                System.exit(3);
            }

            Iterable<Map<String, String>> stops = feed.get("stops.txt");
            if (stops != null) {
                CsvWriter csvWriter = new CsvWriter("stops.txt", ',', UTF8);
                try {
                    csvWriter.writeRecord(new String[]{"stop_id", "stop_code", "stop_name",
                            "stop_lat", "stop_lon", "location_type", "parent_station",
                            "stop_timezone", "wheelchair_boarding", "platform_code", "zone_id"});
                    for (Map<String, String> row : stops) {
                        Stop stop = null; //new Stop(row);
                        final String fields[] = new String[11];
                        fields[0] = stop.stop_id;
                        fields[1] = stop.stop_code;
                        fields[2] = stop.stop_name;
                        fields[3] = stop.stop_lat;
                        fields[4] = stop.stop_lon;
                        fields[5] = stop.location_type;
                        fields[6] = stop.parent_station;
                        fields[7] = stop.stop_timezone;
                        fields[8] = stop.wheelchair_boarding;
                        fields[9] = row.get("platform_code");
                        fields[10] = stop.zone_id;
                        csvWriter.writeRecord(fields);
                    }
                } catch (Exception e) {
                    e.printStackTrace();

                    System.exit(2);
                } finally {
                    csvWriter.close();
                }
            } else {
                System.exit(3);
            }

            Iterable<Map<String, String>> routes = feed.get("routes.txt");
            if (routes != null) {
                CsvWriter csvWriter = new CsvWriter("routes.txt", ',', UTF8);
                Iterable<Route> iterable = Iterables.transform(routes,
                        new Function<Map<String, String>, Route>() {
                            @Override
                            public Route apply(Map<String, String> row) {
                                return null; //new Route(row);
                            }
                        });
                try {
                    csvWriter.writeRecord(new String[]{"route_id", "agency_id", "route_short_name",
                            "route_long_name", "route_desc", "route_type", "route_color",
                            "route_text_color", "route_url"});
                    for (Route route : iterable) {
                        final String fields[] = new String[9];
                        fields[0] = route.route_id;
                        fields[1] = route.agency_id;
                        fields[2] = route.route_short_name;
                        fields[3] = route.route_long_name;
                        fields[4] = route.route_desc;
                        fields[5] = Integer.toString(route.route_type);
                        fields[6] = route.route_color;
                        fields[7] = route.route_text_color;
                        fields[8] = route.route_url;
                        csvWriter.writeRecord(fields);
                    }
                } catch (Exception e) {
                    e.printStackTrace();

                    System.exit(2);
                } finally {
                    csvWriter.close();
                }
            } else {
                System.exit(3);
            }

            Iterable<Map<String, String>> trips = feed.get("trips.txt");
            if (trips != null) {
                CsvWriter csvWriter = new CsvWriter("trips.txt", ',', UTF8);
                try {
                    csvWriter.writeRecord(new String[]{"route_id", "service_id", "trip_id",
                            "realtime_trip_id", "trip_headsign", "trip_short_name",
                            "trip_long_name", "direction_id", "block_id", "shape_id",
                            "wheelchair_accessible" ,"bikes_allowed"});
                    for (Map<String, String> row : trips) {
                        Trip trip = new Trip(row);
                        final String fields[] = new String[12];
                        fields[0] = trip.route_id;
                        fields[1] = trip.service_id;
                        fields[2] = trip.trip_id;
                        fields[3] = row.get("realtime_trip_id");
                        fields[4] = trip.trip_headsign;
                        fields[5] = trip.trip_short_name;
                        fields[6] = row.get("trip_long_name");
                        fields[7] = trip.direction_id;
                        fields[8] = trip.block_id;
                        fields[9] = trip.shape_id;
                        fields[10] = trip.wheelchair_accessible;
                        fields[11] = trip.bikes_allowed;
                        csvWriter.writeRecord(fields);
                    }
                } catch (Exception e) {
                    e.printStackTrace();

                    System.exit(2);
                } finally {
                    csvWriter.close();
                }
            } else {
                System.exit(3);
            }

            Iterable<Map<String, String>> stop_times = feed.get("stop_times.txt");
            if (stop_times != null) {
                CsvWriter csvWriter = new CsvWriter("stop_times.txt", ',', UTF8);
                try {
                    csvWriter.writeRecord(new String[]{"trip_id", "stop_sequence", "stop_id",
                            "stop_headsign", "arrival_time", "departure_time", "pickup_type",
                            "drop_off_type", "timepoint", "shape_dist_traveled",
                            "fare_units_traveled"});
                    for (Map<String, String> row : stop_times) {
                        StopTime stopTime = new StopTime(row);
                        final String fields[] = new String[11];
                        fields[0] = stopTime.trip_id;
                        fields[1] = stopTime.stop_sequence;
                        fields[2] = stopTime.stop_id;
                        fields[3] = stopTime.stop_headsign;
                        fields[4] = stopTime.arrival_time;
                        fields[5] = stopTime.departure_time;
                        fields[6] = stopTime.pickup_type;
                        fields[7] = stopTime.drop_off_type;
                        fields[8] = row.get("timepoint");
                        fields[9] = stopTime.shape_dist_traveled;
                        fields[10] = row.get("fare_units_traveled");
                        csvWriter.writeRecord(fields);
                    }
                } catch (Exception e) {
                    e.printStackTrace();

                    System.exit(2);
                } finally {
                    csvWriter.close();
                }
            } else {
                System.exit(3);
            }

            Iterable<Map<String, String>> calendar_dates = feed.get("calendar_dates.txt");
            if (calendar_dates != null) {
                CsvWriter csvWriter = new CsvWriter("calendar_dates.txt", ',', UTF8);
                Iterable<CalendarDate> iterable = Iterables.transform(calendar_dates,
                        new Function<Map<String, String>, CalendarDate>() {
                            @Override
                            public CalendarDate apply(Map<String, String> row) {
                                return new CalendarDate(row);
                            }
                        });
                try {
                    csvWriter.writeRecord(new String[]{"service_id", "date", "exception_type"});
                    for (CalendarDate calendarDate : iterable) {
                        final String fields[] = new String[3];
                        fields[0] = calendarDate.service_id;
                        fields[1] = calendarDate.date;
                        fields[2] = calendarDate.exception_type;
                        csvWriter.writeRecord(fields);
                    }
                } catch (Exception e) {
                    e.printStackTrace();

                    System.exit(2);
                } finally {
                    csvWriter.close();
                }
            } else {
                System.exit(3);
            }

            Iterable<Map<String, String>> shapes = feed.get("shapes.txt");
            if (shapes != null) {
                CsvWriter csvWriter = new CsvWriter("shapes.txt", ',', UTF8);
                Iterable<Shape> iterable = Iterables.transform(shapes,
                        new Function<Map<String, String>, Shape>() {
                            @Override
                            public Shape apply(Map<String, String> row) {
                                return new Shape(row);
                            }
                        });
                try {
                    csvWriter.writeRecord(new String[]{"shape_id", "shape_pt_sequence",
                            "shape_pt_lat", "shape_pt_lon", "shape_dist_traveled"});
                    for (Shape shape : iterable) {
                        final String fields[] = new String[5];
                        fields[0] = shape.shape_id;
                        fields[1] = shape.shape_pt_sequence;
                        fields[2] = shape.shape_pt_lat;
                        fields[3] = shape.shape_pt_lon;
                        fields[4] = shape.shape_dist_traveled;
                        csvWriter.writeRecord(fields);
                    }
                } catch (Exception e) {
                    e.printStackTrace();

                    System.exit(2);
                } finally {
                    csvWriter.close();
                }
            } else {
                System.exit(3);
            }

            Iterable<Map<String, String>> transfers = feed.get("transfers.txt");
            if (transfers != null) {
                CsvWriter csvWriter = new CsvWriter("transfers.txt", ',', UTF8);
                try {
                    csvWriter.writeRecord(new String[]{"from_stop_id", "to_stop_id",
                            "from_route_id", "to_route_id", "from_trip_id", "to_trip_id",
                            "transfer_type"});
                    for (Map<String, String> row : transfers) {
                        Transfer transfer = new Transfer(row);
                        final String fields[] = new String[7];
                        fields[0] = transfer.from_stop_id;
                        fields[1] = transfer.to_stop_id;
                        fields[2] = row.get("from_route_id");
                        fields[3] = row.get("to_route_id");
                        fields[4] = row.get("from_trip_id");
                        fields[5] = row.get("to_trip_id");
                        fields[6] = transfer.transfer_type;
                        csvWriter.writeRecord(fields);
                    }
                } catch (Exception e) {
                    e.printStackTrace();

                    System.exit(2);
                } finally {
                    csvWriter.close();
                }
            } else {
                System.exit(3);
            }

            Iterable<Map<String, String>> feed_info = feed.get("feed_info.txt");
            if (feed_info != null) {
                CsvWriter csvWriter = new CsvWriter("feed_info.txt", ',', UTF8);
                try {
                    csvWriter.writeRecord(new String[]{"feed_publisher_name",
                            "feed_id", "feed_publisher_url", "feed_lang", "feed_start_date",
                            "feed_end_date", "feed_version"});
                    for (Map<String, String> row : feed_info) {
                        FeedInfo feedInfo = new FeedInfo(row);
                        final String fields[] = new String[7];
                        fields[0] = feedInfo.feed_publisher_name;
                        fields[1] = row.get("feed_id");
                        fields[2] = feedInfo.feed_publisher_url;
                        fields[3] = feedInfo.feed_lang;
                        fields[4] = feedInfo.feed_start_date;
                        fields[5] = feedInfo.feed_end_date;
                        fields[6] = feedInfo.feed_version;
                        csvWriter.writeRecord(fields);
                    }
                } catch (Exception e) {
                    e.printStackTrace();

                    System.exit(2);
                } finally {
                    csvWriter.close();
                }
            } else {
                System.exit(3);
            }
            System.out.printf("Work done after %.9f seconds.\n", (System.nanoTime() - time) * 1e-9);
        }
    }
}
