# Dictionary - OTP Internal Domain Language

The target audience for this document are developers and product owners working with OTP. API and data-feed specific terminology is excluded, unless it is relevant for understanding the internal model.

Usually, we use transit terms from
the [GTFS specification](https://gtfs.org/documentation/schedule/reference/#) when possible. Terms
from [https://transmodel-cen.eu/](Transmodel specification) are used for NETEX/Transmodel specific
concepts, or when we do not feel like the GTFS terms are the most optimal. For rental, we use terms
from the [GBFS specification](https://gbfs.org/documentation/). For algorithms, we use terminology
from scientific literature.

This document includes terms we use for concepts that are not fully covered by the aforementioned
specifications or when we find it important to clarify what the terms mean in OTP's context. Note,
this document will be updated over time, and it does not yet cover everything it should cover.

## Dates

| Term         | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
|--------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Running date | The actual calendar date. One departure can start and end on a different running date. A trip or trip-pattern may have more than one running date. If the first departure is 2025-01-31 and the last arrival is on 2025-02-02, then 2025-01-31, 2025-02-01 and 2025-02-02 are all running dates for the given trip.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| Service date | Service dates are used in transit data and they are partly based on how the transit authorities operate. They typically in practice start and end later than a running date, but the starting point for the time of the service date is noon - 12 hours. For example, a trip starting at 1am is often still considered to be part of the previous "calendar date's" service date and in service date terminology, the starting time would be 25:00. There can be trips that last for 24+ hours, but each departure only has one service date and its chosen based on when the trip starts. |

## Stop references

| Term               | Description                                                                                                                                                              |
|--------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Stop id                | A global unique id, frequently used by the APIs and data feeds to reference a stop   |
| Stop position      | Zero-based index, incremented by 1 for each stop in the stop pattern.                                                                                                    |
| GTFS stop sequence | Comes from the GTFS data. Can be used for real-time data matching but also be fetched from the APIs.                                                                        |
| Stop index         | The global integer index of stops used internally in OTP and in Raptor to reference stops by a single integer. There is a 1-to-1 mapping between stop index and Stop:id. |
