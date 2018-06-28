## Planing parameters
| Parameter | OTP Equivalent | In use | Difficulty |
| --- | --- | --- | --- |
| Origin | fromPlace | Y | 0 |
| Destination | toPlace | Y | 0 |
| Vias | intermediatePlaces. OTP only supports lat/lon locations while Jeppense allows stopIds to be specified as well | N | 3 |
| NotVias | bannedStopsHard | N | 0 |
| JourneyTime | time | N | 0 |
| JourneyTimeMode | Jeppensen provides 4 modes: leaveAfter, arriveBy, first and last. OTP only supports arriveBy and leaveAfter (by setting arriveBy to either False or True). It might be non-trivial to add support for other modes | Not sure | 5 |
| DataSet | Router id | Y | 0 |
| MapDataContent | not supported | Not sure | 5 |
| TimeoutSeconds | not supported | Y | 2 |
| MaxWalkDistanceMetres | maxWalkDistance | Y | 0 |
| IsFareDataReturned | not sure | not sure |  3 |
| TransactionId | not supported | not sure | not sure |
| MaxJourneys | numItineraries | Y | 0 |
| WalkSpeed | walkSpeed | Y | 0 |
| MaxChanges | can be supported by maxTransfers but the calculation of changes/transfers in OTP will need to change | Y | 3 |
| PermittedTransportModes | modes | Y | 0 |
| PermittedServiceProviders | OTP uses `preferredAgencies` which relaxes this constraint a bit by giving penalty to other non-preferred agencies | Y | 3 |
| RealTimeMode | ignoreRealtimeUpdates (require clarification) | Not sure | 1 |
| JourneyStartingModes | OTP uses bikeParkAndRide and parkAndRide. OTP offers extra kissAndRide, plus cost/time control for each mode | Not sure | 2 |
| JourneyStartAttributes | somewhat supported by maxPreTransitTime | not sure | not sure |
| IsWheelChairAccessRequired | wheelchair | Y | 0 |
| PermittedRoutes | preferredRoutes or another custom field | not sure | 3 |
| ForbiddenTransportModes | negativty of PermittedTransportModes | Y | 2 |
| ForbiddenServiceProviders | bannedAgencies (require confirmation) | Not sure | 1 |
| ForbiddenRoutes | bannedRoutes (require confirmation) | Not sure | 1 |
| ForbiddenRoadTypes | not supported | Not sure | 5 |
| ForbiddenRoadAttributes | not supported | Not sure | 5 |
| IncludeOvertakenJourneys | not supported | not sure | not sure |
| ReturnNotes | not supported | not sure | not sure |
| InterchangeTimeMultiplier | transferSlack might be suitable | not sure | 3 |
| MinInterchangeMinutes | transferSlack might be suitable | not sure | 3 |
| MaxConnectionMinutes | not supported | not sure | not sure |
| ExcludePeak | not supported | not sure | not sure |
| WalkOnlyJourney | set mode to Walk only or alternatively set transit to false | Y | 2 |

## Issues
* Pathways.txt: for entrances and pedestrian pathways (transperth gtfs doesnt have this)
* Max transfers: resolved
* Timepoint flag on stops: resolved
* Selective agencies: Jacob mentioned we might not need this.
* Broken timetable: TEST THIS
* Multiple (independent) service providers: For cases in which there are independent service providers operating in the same geographical region (for example Transperth and AmristarBus operating in Perth), those providers might have independent GTFS feeds. In OTP, a feed is converted to an isolated graph/tree. By default, OTP does not perform cross-graph route search. This means that you can only get routes from a feed at a time. The behaviour might be undesirable if Transperth would like school bus (from different GTFS feeds) to be included in the route search as well. Merging GTFS feeds is a possible solution but care should be taken to avoid id collisions. UPDATE: Transperth seems to combine school bus feeds with theirs. There is a new filtering tag for school buses `school_only` in routes.txt. --> SOLVED
* Wheelchair accessible: currently treated as non criticial filter in OTP. This can be easily changed to a critical filter if required.
* Check how vias is currently implemented in Jepensen
* Set default for maxWalkDistance
* Consider the L option for landmark (strict starting point):  RoutingContext
if (opt.startingTransitStopId != null) {
            Stop stop = graph.index.stopForId.get(opt.startingTransitStopId);
            TransitStop tstop = graph.index.stopVertexForStop.get(stop);
            startingStop = tstop.departVertex;
        }
alternatively, in SimpleStreetSpliter routine to find the starting and ending vertex (given long lat), snap the starting and ending vertex to stop vertex. In RoutingContext, also consider set fromVertex = orign and toVertex = target
    
* Optimization to improve search speed and loading time --> private RoutingContext(RoutingRequest routingRequest, Graph graph, Vertex from, Vertex to, boolean findPlaces)
* OTP uses OSM data to calculate distance
* Mod auth token
* i.tooSloped = request.rctx.slopeRestrictionRemoved: warn user about relaxed slope restriction when searching for accessible trips

## Notes:
* Current IPTIS v7 with Jepensen backend does not render school route correctly.
* SJP 106 is school bus mode
