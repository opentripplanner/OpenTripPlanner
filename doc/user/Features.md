# Feature Overview

## Routing Algorithms and Transit

**Range Raptor Algorithm**
- Core transit routing engine based on Microsoft's Raptor algorithm with multi-criteria pareto-optimal search. Provides fast, efficient transit routing with support for multiple optimization criteria.

**Dynamic Search Window**
- Automatically calculates optimal search windows for transit routing based on heuristics, improving performance while maintaining result quality.

**Transfer Optimization**
- Optimizes transfer locations and waiting times between transit trips to reduce back-travel and improve overall journey quality.

**Itinerary Filtering**
- Advanced filter chain that removes dominated itineraries, groups similar results, and applies various cost-based filters to return only the most relevant trip options.

**Multi-criteria Optimization**
- Supports balancing multiple factors including travel time, number of transfers, walking distance, safety, and cost when computing routes.

**Parallel Search**
- Configurable thread pool for splitting transit searches into smaller jobs that run in parallel to improve performance on large deployments.

**Paging Support**
- Allows fetching next/previous page of results with dynamic search window adjustments for smooth pagination.

## Street Routing and Modes

**Walking**
- Full pedestrian routing with configurable speed, reluctance, stair preferences, and elevation consideration. Supports escalator modeling and safety factors.

**Cycling**
- Bicycle routing with triangle optimization (safety, flatness, time), bike-friendly street detection, and elevation consideration.

**Driving**
- Car routing with acceleration/deceleration modeling, turn reluctance, and support for park-and-ride scenarios.

**Bike Rental (Bikeshare)**
- Integration with bike-sharing systems via GBFS, supporting both station-based and free-floating bikes.

**E-Scooter Rental**
- Support for scooter-sharing systems with configurable speed and reluctance parameters.

**Car Rental**
- Car-sharing integration for both fixed-location and free-floating car rental services.

**Bike-to-Park**
- Support for cycling to a station, parking the bicycle, and continuing via transit.

**Car-to-Park (Park & Ride)**
- Park-and-ride functionality with automatic detection of parking facilities from OSM data.

**Kiss & Ride**
- Drop-off scenarios where a car is used to reach a transit station but not parked.

**Car Hailing**
- Integration with ride-hailing services (Uber, Lyft) for first/last mile connections.

**Car Pickup**
- Walking to a pickup point, driving to a drop-off point, and walking the rest of the way.

## Transit Data Support

**GTFS Import**
- Full support for GTFS (General Transit Feed Specification) including calendars, frequencies, fares, transfers, and shapes.

**NeTEx Import**
- Support for Nordic Profile of NeTEx, the EU-standard transit data interchange format. Can mix GTFS and NeTEx data in a single graph.

**Block-based Interlining / NeTEx stay-seated interchanges**
- Automatic detection of stay-seated transfers between trips with the same block ID.

**Station Transfers**
- Pre-calculated transfers between stops, with configurable preferences for station-based transfers.

**Multiple Feed Support**
- Load and combine transit data from multiple agencies.

**Service Calendar Filtering**
- Limit transit service period to specific date ranges to optimize graph size and build time.

**Trip Shapes**
- Support for GTFS shapes to provide accurate route geometry for visualization.

## Real-time Data Updates

**GTFS-RT Trip Updates**
- Real-time timetable updates showing delays, cancellations, and schedule modifications via GTFS Realtime.

**GTFS-RT Vehicle Positions**
- Real-time vehicle location tracking for display on maps and enhanced passenger information.

**GTFS-RT Service Alerts**
- Real-time service alerts and disruption notifications affecting routes, stops, or trips.

**SIRI-ET (Estimated Timetables)**
- European real-time standard for estimated arrival/departure times with support for both HTTP polling and Lite variants.

**SIRI-SX (Situation Exchange)**
- Real-time alert information using the SIRI standard, common in European deployments.

**SIRI-FM (Facility Monitoring)**
- Real-time parking facility status updates via SIRI standard.

**MQTT Support**
- Subscribe to GTFS-RT updates via MQTT message queues for efficient real-time data ingestion.

**Vehicle Rental Updates**
- Real-time availability updates for bike and scooter rental systems via GBFS feeds.

**Vehicle Parking Updates**
- Real-time parking availability from multiple sources including Liipi, ParkAPI, Bikeep, and Bikely.

**Timetable Snapshot Management**
- Intelligent caching and throttling of real-time timetable snapshots to optimize performance.

**Backwards Delay Propagation**
- Configurable propagation of delays backwards through trip patterns when upstream data is missing.
[](Is this correct? Does this only apply to SIRI)

## Accessibility Features

**Wheelchair Routing**
- Full wheelchair-accessible route calculation with configurable costs for unknown and inaccessible entities.

**Slope Restrictions**
- Maximum slope limits for wheelchair users with penalties for exceeding thresholds.

**Elevator Accessibility**
- Separate accessibility configuration for elevators with boarding time and costs.

**Accessible Transfers**
- Pre-calculation of wheelchair-accessible transfers between stops during graph build.

**Inaccessible Street Penalties**
- High reluctance values for streets marked as wheelchair-inaccessible to avoid them except as last resort.

**Stop and Trip Accessibility**
- Honors GTFS wheelchair_accessible fields for stops and trips with configurable unknown/inaccessible costs.

## APIs and Interfaces

**GTFS GraphQL API**
- Production-ready GraphQL API using GTFS vocabulary, used by Digitransit and otp-react-redux.

**Transmodel GraphQL API**
- GraphQL API using Transmodel/NeTEx vocabulary, used by Entur in production since 2020.

**Rate Limiting**
- Query complexity-based rate limiting for GraphQL APIs to prevent overload.

**API Tracing**
- Correlation ID support and request tracing for monitoring across microservices.

**API Timeouts**
- Configurable maximum processing time for API requests to prevent long-running queries.

## Data Sources and Formats

**OpenStreetMap (OSM)**
- Full OSM import for street network with support for PBF, XML formats and custom tag mappings.

**Custom OSM Tag Mappings**
- Country-specific OSM tag interpreters (Norway, UK, Finland, Germany, Hamburg, Atlanta, Houston, Portland).

**Digital Elevation Models (DEM)**
- Support for GeoTIFF elevation data with configurable unit conversion and geoid adjustments.

**Elevation Caching**
- Reusable elevation calculation cache to speed up subsequent graph builds.

**Google Cloud Storage**
- Direct loading of input files (GTFS, NeTEx, OSM, DEM) from Google Cloud Storage buckets.

**Elevation Support**
- Support for NED elevation tiles for better walk and bicycle routing.

**Flexible Data Sources**
- Support for loading all input data types from file/directories, HTTP/HTTPS URLs, Gand oogle Cloude Storage.

## Optimization and Performance

**Pre-calculated Transfers**
- Graph-build-time calculation of walking transfers between nearby stops for multiple street modes.

**Transfer Cache**
- Runtime caching of calculated transfers with configurable cache size to improve query performance.

**Island Pruning**
- Automatic detection and removal of disconnected street graph segments that cause routing problems.

**Adaptive Pruning**
- Distance-based threshold adjustment for more aggressive pruning of problematic disconnected areas.

**Multi-threaded Graph Building**
- Parallel processing during elevation calculations to speed up graph construction.

**OSM In-Memory Caching**
- Option to cache OSM data in memory during processing instead of streaming multiple passes.

**Binary Trip Search**
- Optimized trip departure time lookup using binary search for better performance on high-frequency routes.

**Search Thread Pool**
- Split transit searches across multiple threads for improved response times on complex queries.

## Street Network Features

**Turn Restrictions**
- Support for OSM turn restrictions and configurable turn reluctance.

**One-way Streets**
- Proper handling of one-way streets and directional restrictions from OSM.

**Traffic Signals**
- Intersection traversal cost modeling including traffic signals and turn penalties.

**Area Visibility**
- Calculate paths straight through OSM areas rather than around edges (computationally expensive).

**Platform Entrances**
- Link unconnected platform entrances to the street network for better station access.

**Boarding Locations**
- Match transit stops to OSM platforms and entrances using configurable tag patterns.

**Bike Parking**
- Static bicycle parking locations from OSM data for bike-to-park scenarios.

**Car Parking**
- Static car parking locations and park-and-ride facilities from OSM data.

**Time-restricted Access**
- Support for OSM opening hours on paths and streets that vary by time of day.

**Walk Safety**
- Safety scoring for pedestrian routes based on OSM tagging (bike lane, sidewalks, traffic).

**Bike Safety**
- Safety scoring for cycling routes based on infrastructure quality and traffic levels.

## Multimodal Capabilities

**Combined Journey Planning**
- Seamless combination of walking, cycling, driving, transit, and mobility services in a single itinerary.

**First/Last Mile Integration**
- Various access/egress modes (walk, bike, rental vehicles, car) to reach transit.

**Mode-specific Configurations**
- Fine-grained control over board/alight slack, reluctance, and costs per transit mode (bus, rail, ferry, etc.).

**Transit with Bicycles**
- Support for taking bicycles on transit when allowed by the feed data.

**Demand-Responsive Transit (Flex)**
- Full support for GTFS Flex (on-demand, deviated route, and zone-based services).

**Vehicle Rental Integration**
- Seamless integration of bike, scooter, and car rental into multimodal journeys.

## Fares and Costs

**GTFS Fares V1**
- Standard GTFS fare calculation based on fare_attributes and fare_rules.

**GTFS Fares V2**
- Experimental support for the new Fares V2 specification including fare products and media.

**Interagency Fares**
- Calculate fares across multiple transit agencies in the same trip.

**Custom Fare Calculators**
- Pluggable fare calculation system with custom implementations (Atlanta, HSL, ORCA, highest-fare-in-window, combined-interlined-legs).

**Generalized Cost**
- Unified cost model combining time, monetary cost, discomfort, and other factors for route comparison.

## Configuration and Deployment

**Multiple Configuration Levels**
- Separate configs for OTP features (otp-config.json), graph building (build-config.json), and routing (router-config.json).

**Feature Toggles**
- Enable/disable experimental and sandbox features via configuration.

**Config Versioning**
- Track deployment config versions for verification and debugging.

**Embedded Router Config**
- Option to embed router configuration in the graph for fully self-contained deployments.

**Container Deployment**
- Docker/container-ready with health check endpoints and environment variable support.

**Serialized Graphs**
- Save and load pre-built graphs to eliminate rebuild time in production deployments.

## Monitoring and Operations

**Build Reports**
- Detailed HTML reports of graph build process with errors, warnings, and statistics.

**Data Import Reports**
- Comprehensive validation reports for GTFS, NeTEx, and OSM data quality issues.

**Logging and MDC**
- Structured logging with Mapped Diagnostic Context for correlation IDs and request tracking.

**Request Tracing**
- HTTP header-based request tracing throughout the application for debugging and monitoring.

## Advanced Routing Features

**Via Points**
- Route through specific intermediate waypoints while optimizing overall journey.

**Arrive-by vs. Depart-at**
- Bi-directional search supporting both departure-time and arrival-time optimization.

**Unpreferred Routes/Agencies**
- Penalize specific routes or agencies while still allowing them as options.

**Banned Routes/Agencies**
- Completely exclude specific routes or agencies from search results.

**Mode Restrictions**
- Filter allowed transit modes, access modes, and direct modes per request.

**Transit Group Priority**
- Group transit patterns to ensure routes using different operators or service types are both returned as optimal results, despite cost differences.

**Transit Reluctance**
- Mode-specific reluctance factors to prefer certain transit types over others.

**Walk Limits**
- Configurable maximum walk distances and times for access, egress, and transfers.

**Transfer Limits**
- Control maximum number of transfers and minimum safe transfer times.

## Visualization and Analysis

**Debug Visualization**
- Built-in web-based graph visualization tool for inspecting the street and transit network.

**Inspector API**
- JSON endpoints for querying graph structure, stops, patterns, and edges.

**Route Color Mapping**
- Preserve GTFS route colors for consistent visualization across applications.

**Pattern Geometry**
- Accurate trip pattern shapes for map display derived from GTFS shapes or stop-to-stop connections.

---

## Sandbox Features (Experimental/Community-Maintained)

### Sandbox: APIs and Interfaces

**Vector Tiles API**
- Mapbox vector tiles for stops, stations, rental vehicles, and parking facilities. Used by Digitransit UI and otp-react-redux.

**Actuator API**
- Spring Boot-style health check and Prometheus metrics endpoints for monitoring OTP instances.

**Geocoder API**
- Geocoding of stop names and codes for location search.

**Report API**
- Access graph build reports and validation issues via API.

**Park and Ride API**
- Dedicated endpoints for querying park-and-ride facilities.

**TRIAS API**
- German standard TRIAS (Traveller Information Architecture Standard) API support.

### Sandbox: Real-time Updaters

**SIRI Azure Message Bus**
- Receive SIRI-ET and SIRI-SX updates via Azure Service Bus with historical data support.

**SIRI Google Cloud Pub/Sub**
- Subscribe to SIRI-ET updates via Google Cloud Pub/Sub messaging.

**SIRI-ET Lite**
- Lightweight SIRI Estimated Timetables support for simple deployments.

**SIRI-SX Lite**
- Lightweight SIRI Situation Exchange for service alerts.

**Vehicle Rental Service Directory**
- Automatically discover and load vehicle rental feeds from a central directory service.

**Smoove Bike Rental**
- Integration with Smoove bike rental systems.

### Sandbox: Data and Calculations

**Emissions (CO2)**
- Calculate and display CO2 emissions per route or trip segment. Supports both route-level and trip-hop-level emission data.

**Empirical Delay**
- Learn from historical delay patterns to improve real-time predictions and routing decisions.

**Fares (Extended)**
- Advanced fare calculation with custom implementations for specific agencies and regions.

**Data Overlay**
- Apply data overlays to modify graph properties without rebuilding.

### Sandbox: Specialized Features

**Flexible Transit Routing (Flex)**
- Full support for GTFS Flex demand-responsive services including zone-based, deviated-route, and on-demand transit.

**Ride Hailing**
- Integration with Uber API for real-time ride-hailing cost and time estimates.

**IBI Accessibility Score**
- Experimental accessibility scoring system for rating itinerary wheelchair accessibility (0-1 scale).

**Stop Consolidation**
- Merge nearby stops into logical groups for cleaner user interfaces and simpler trip planning.

**Google Cloud Storage Integration**
- Enhanced support for loading all data types from GCS with authentication configuration.

**Transfer Analyzer**
- Tools for analyzing and visualizing transfer patterns in the network.

**Interactive OTP Main**
- Interactive command-line interface for debugging and development.

**Debug Raster Tiles**
- Raster tile endpoint for debugging spatial data issues.

**Vehicle Parking (Extended)**
- Enhanced vehicle parking support with multiple data source integrations (Liipi, ParkAPI, Bikeep, Bikely, SIRI-FM).

**Sorlandsbanen**
- Specialized features for specific Norwegian railway line requirements.
