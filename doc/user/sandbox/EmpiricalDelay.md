# Empirical Delay

## Contact Info

- Entur AS

## Documentation

The empirical delay module enables import of historical delay data which OTP can use to provide
typical delay information based on actual observed service delays. Empirical delay data is loaded
during the graph build process and provides percentile-based delay estimates (p50 and p90) for
service journeys at specific stops.

This feature helps users understand the typical reliability of a service based on historical data,
allowing them to make more informed travel decisions. For example, a service that historically
arrives 5 minutes late 50% of the time (p50 = 5 minutes) gives travelers a more realistic
expectation than just the scheduled time.

### Empirical Delay Data Files

Empirical delay data is provided through two CSV files that must be supplied as standalone files
listed in the `build-config.json`. Both files must be provided together:

- `empirical_delay_stop_times.txt` - Contains delay percentiles for each trip stop
- `empirical_delay_calendar.txt` - Defines service calendars for the delay data

The file names must include "empirical_delay" and the extension must be `.csv` or `.txt`. Both
NeTEx and GTFS feeds are supported through the feedId configuration.

### Empirical Delay Stop Times Format

This file contains the delay data for each stop on each trip, organized by service calendar periods (e.g., weekdays, weekends).

CSV file columns:

| CSV Header                     | Description                                                                                             |
|:-------------------------------|:--------------------------------------------------------------------------------------------------------|
| `empirical_delay_service_id`   | The service calendar identifier that determines when this delay data applies (references calendar file). |
| `trip_id`                      | The GTFS trip id or NeTEx ServiceJourney id.                                                            |
| `stop_id`                      | The GTFS stop id or NeTEx Quay id for this stop.                                                        |
| `stop_sequence`                | The stop sequence number in the trip pattern. First stop is 1, not 0.                                   |
| `p50`                          | The median (50th percentile) delay in seconds. Half of historical observations are better than this.    |
| `p90`                          | The 90th percentile delay in seconds. 90% of observations are better, 10% are worse.                    |

**Constraints:**
- Percentile values (p50, p90) must be between 0 and 18000 seconds (5 hours)
- Stop sequence must be between 0 and 10,000
- If the stop_id does not match the transit feed, the row is dropped and an issue is added to the
  build report

Example:

```csv
empirical_delay_service_id,trip_id,stop_id,stop_sequence,p50,p90
Friday,RUT:ServiceJourney:1,NSR:Quay:1001,1,0,30
Friday,RUT:ServiceJourney:1,NSR:Quay:1002,2,120,240
Friday,RUT:ServiceJourney:1,NSR:Quay:1003,3,180,360
Saturday,RUT:ServiceJourney:1,NSR:Quay:1001,1,30,60
Saturday,RUT:ServiceJourney:1,NSR:Quay:1002,2,45,120
```

In this example:
- On Fridays, stop 2 has a median delay of 2 minutes (120s) and is delayed by 4+ minutes 10% of the
  time
- On Saturdays, the same stop has better performance with only 45 seconds median delay

### Empirical Delay Calendar Format

This file defines service calendars that determine when specific delay patterns apply, similar to
GTFS calendar.txt format.

CSV file columns:

| CSV Header                     | Description                                                        |
|:-------------------------------|:-------------------------------------------------------------------|
| `empirical_delay_service_id`   | Unique identifier for this service calendar.                       |
| `monday`                       | Boolean (0 or 1) - whether this calendar applies on Mondays.       |
| `tuesday`                      | Boolean (0 or 1) - whether this calendar applies on Tuesdays.      |
| `wednesday`                    | Boolean (0 or 1) - whether this calendar applies on Wednesdays.    |
| `thursday`                     | Boolean (0 or 1) - whether this calendar applies on Thursdays.     |
| `friday`                       | Boolean (0 or 1) - whether this calendar applies on Fridays.       |
| `saturday`                     | Boolean (0 or 1) - whether this calendar applies on Saturdays.     |
| `sunday`                       | Boolean (0 or 1) - whether this calendar applies on Sundays.       |
| `start_date`                   | Start date in YYYY-MM-DD format (inclusive).                       |
| `end_date`                     | End date in YYYY-MM-DD format (inclusive).                         |

Example:

```csv
empirical_delay_service_id,monday,tuesday,wednesday,thursday,friday,saturday,sunday,start_date,end_date
Weekday,1,1,1,1,1,0,0,2025-01-01,2030-12-31
Weekend,0,0,0,0,0,1,1,2025-01-01,2030-12-31
Friday,0,0,0,0,1,0,0,2025-01-01,2030-12-31
```

### Configuration

To enable this functionality, you need to enable the "EmpiricalDelay" feature in the 
`otp-config.json` file:

```JSON
// otp-config.json
{
  "EmpiricalDelay": true
}
```

Include the `empiricalDelay` object in the `build-config.json` file with a list of feeds containing
empirical delay data:

```JSON
// build-config.json
{
  "empiricalDelay": {
    "feeds": [
      {
        "feedId": "RUT",
        "source": "https://example.org/empirical-delay/rut-delays.zip"
      },
      {
        "feedId": "NSB",
        "source": "file:///data/empirical-delay/nsb-delays.zip"
      }
    ]
  }
}
```

Each feed configuration requires:
- `feedId` (required): The feed ID to use for matching transit IDs in the empirical delay data. 
  This must match the feed ID of the corresponding GTFS or NeTEx feed.
- `source` (required): URI pointing to the empirical delay data. Can be a local file, HTTP(S) URL,
  or cloud storage URI (e.g., gs://).

The source can be:
- A ZIP file containing both `empirical_delay_stop_times.txt` and `empirical_delay_calendar.txt`
- A directory containing these files
- Individual files (both must be provided)

### GraphQL API

The empirical delay data is exposed through the Transmodel GraphQL API on the `EstimatedCall` type:

```graphql
type EstimatedCall {
  # ... other fields ...

  "The typical delay for this trip on this day for this stop based on historical data."
  empiricalDelay: EmpiricalDelay
}

type EmpiricalDelay {
  "The median/50% percentile. This value is in the middle of the distribution."
  p50: Duration
  "The 90% percentile. 90% of the values in the distribution is better and 10% is more delayed."
  p90: Duration
}
```

Example query:

```graphql
{
  stopPlace(id: "NSR:StopPlace:1") {
    estimatedCalls(numberOfDepartures: 5) {
      expectedDepartureTime
      destinationDisplay {
        frontText
      }
      empiricalDelay {
        p50
        p90
      }
    }
  }
}
```

If no empirical delay data is available for the specific combination, the field returns `null`.

## Use Cases

### Traveler Information

Empirical delay data can be displayed to travelers to set realistic expectations:
- "This service is typically 2-3 minutes late at this stop"
- "90% of the time, this bus arrives within 5 minutes of schedule"

### Journey Planning

Journey planners can use empirical delay to:
- Add realistic buffer times when suggesting transfers
- Rank alternatives based on historical reliability
- Adjust arrival time predictions beyond real-time data

### Service Quality Monitoring

Transit operators can:
- Identify consistently delayed services
- Compare different time periods (weekday vs weekend)
- Track improvement over time as new delay data is imported

## Changelog

### OTP 2.9

- Initial implementation of empirical delay with percentile-based delay data (p50, p90), service
  calendar support for day-of-week patterns, and available in the Transmodel GraphQL API.
