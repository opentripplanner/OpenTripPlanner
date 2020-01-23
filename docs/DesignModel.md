## Terminology
This is a description of terminology used in the code - it should describe the conceptual design. Describe the functionality in the code, not here. The purpose of this document is to give a overview of terms in use, and define a common unambiguous language. Use terminology defined here in OTP(code and doc) and not undefined synonyms.  
   

### Search

#### origin and destination
In a regular search we start at the `origin` and end at the `destination`. Sometimes we search in reverse (traversing the graph in the opposite direction), then the `origin` and `destination` is swapped in algorithm(Raptor), but not from a client perspective. Make sure the context is clear when talking about the reverse search.


#### earliest-departure-time (EDT)
The earliest time a journey can start.

#### latest-departure-time (LDT)
The latest time a journey can start. This is derived by: 
```
latest-departure-time := earliest-departure-time + search-window
```
#### earliest-arrival-time (EAT)
The earliest time a journey can arrive. This is derived by:
```
earliest-arrival-time := latest-arrival-time - search-window
```

#### latest-departure-time (LDT)
The latest time a journey can end. 

#### search-window
This is the _depature_ or _arrival_  time-window. 
 
#### travel-window
This is the time window the hole journey must take place within. It is derived by:
```
travel-window := latest-arrival-time - earliest-departure-time
```

### Search direction
We can search `forward` and in `reverse`. A `forward` search searches from the `origin` to `destination` forward in time,  while a `revers` search searches `backwards` in time. From the client perspective a `reverse` search, start at the `destination` and end at the `origin` - but this is not the case in the context of the algorithm. In `OTP1` the `arriveBy=true`  
 
### Departure and arrival preferences

This indicate the end user preference for arrival. There is 3 different modes.

#### depart-after-and-arrive-as-early-as-possible (depart-after)
The user want to arrive as early as possible, but leave _after_ the given `earliest-departure-time`. This is typically used in a scenario were the user can not leave before a given time and want to get to the destination as early as possible. The user want the journey arriving as early as possible, but can not leave before a given time. The user will wait for another trip only if it is better in some other way; Like shorter travel time.
```
   EAT
A:  |  |--xxx-xxxx-|          # Optimal, arrive before all other trips
B:  |       |-xx-xx-|         # Optimal, Travel time is shorter than A and it arrives before C.
C:  |          |-x-x-|        # Optimal, best travel-time (shortest trip)
    |
X:  |            |-xx-xx-|    # NOT optimal, arrive after C and takes longer than C
```
With this mode the `earliest-departure-time` is required.

THIS IS NOT IMPLEMENTED, BUT IS A EASY THING TO DO IF NEEDED.


#### time-table
The traveler want the most convenient travel option - the trip witch is the fastest. Trips witch leaves after another trip and arrive before is preferred. Two trips, A and B, is both optimal (included in the result) if A depart before B, and A arrive before B. The timetable show all optimal trips within the travel window from earliest-departure-time to latest-arrival-time. This it typically used when the user have a time window she/he can travel in, and just want to find the best option in that window. This is also the best option for a ticketing machine, where you select from/to stop and display all option for the next N hours. This optimization ignore the total-travel-time as a criteria.
```
   EDT                        LAT
A:  |  |--xxxx--xxxxx-|        |    # Optimal, arrive before all other trips
B:  |          |--x-xxx-|      |    # Optimal, depart after A and arrive before C
C:  |            |--xxxx-xxx-| |    # Optimal, depart after all other trips
    |                          | 
X:  |       |--xxxx--xxxx-|    |    # NOT optimal, depart before B and arrive after B
```
With this mode the `earliest-departure-time` and `search-window` is normally used as input, but `latest-arrival-time` can also be used. The Raptor Search will dynamically calculate the the EDT, LAT and search-window as needed.

This is the default preference (only one supported at the moment) 


#### depart-after-and-arrive-as-early-as-possible
The user want to depart as late as possible and arrive _before_ the given `latest-arrival-time`. This is typically used in a scenario were the user is going to an event and must be there in time, before it start. The user want the journey leaving as late as possible, but getting there in time. The user will wait for the latest departure, unless one of the other options are better in another criteria (shorter time).

```
                       LAT
A:      |-x-x-|         |    # Optimal, shortest travel time.
B:       |-xx-xxx-|     |    # Optimal, depart after A and travel time is shorter than C.
C:        |-xx-xx-xxx-| |    # Optimal, latest departure
                        |
X:   |-xxx-x-|          |    # NOT optimal, takes longer than A and arrive before C
```
With this mode the `latest-arrival-time` is required.

THIS IS NOT IMPLEMENTED, BUT IS A EASY THING TO DO IF NEEDED.

