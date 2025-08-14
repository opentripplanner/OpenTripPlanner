# Feed ID


The feed ID is a unique identifier given to a GTFS or NeTEx dataset allowing OTP to associate real-time 
feeds with static timetable data.

Some applications don't need to manage more than a single transit dataset at a time, where the static 
data and the real-time feeds are linked directly to one another. However, OTP has the ability and 
is often used for this purpose. Therefore, it is important to understand how feed IDs are used and 
how to configure them properly.

## Determining the `feedId` value

A feed ID is assigned when building a graph, where it will be stored in the graph itself. Thus,
once stored in a graph, a GTFS or NeTEx dataset retains its assigned feedId regardless of where and
when the graph is loaded.

### GTFS

To define a GTFS `feedId` value, OTP uses the three following methods, listed by priority:

* Via `build-config.json`
* Reading non-standard feed_id value from the `feed_info.txt` file
* Automatically assigned value

#### Via `build-config.json`

If specified, OTP uses the provided feedId of a given GTFS feed, listed in the [transitFeeds object from build-config.json](../examples/ibi/portland/build-config.json). 
This allows the user to specify the `feedId` value they want to use.

While this value is optional, and because OTP will either use the specified value or automatically 
assign an unknown one to the user, it is recommended to set a unique feedId value. 
This value could be used later, for example, if real-time feeds need to be added to the static data 
at a later time without having to rebuild the graph.

This is the preferred and most reliable method to configure a GTFS feedId.

#### Reading non-standard feed_id value

When no feedId is provided via the build-config.json file for a given GTFS feed, or when a GTFS 
static data file is simply added to the working directory during build, OTP looks for a feed_id value 
from `feed_info.txt` file, if available.

Note that the `feed_info.txt` file is optional. Furthermore, the feed_id value is non-standard: 
although its addition to the `feed_info.txt` file has been [discussed several times by the GTFS community](https://github.com/google/transit/pull/62), it was never incorporated into the GTFS standard. However, it has since been adopted by a few supporting transit agencies and transit data providers.

#### Automatically assigned value (fallback method)

As a fallback method, OTP assigns a feedId as an increasing integer to all GTFS data processed at build.

This method has several drawbacks. First, if multiple GTFS datasets are used, the value automatically 
assigned by OTP is difficult to predict. Second, OTP could assign a different value if another GTFS 
dataset is added later. While it is possible to identify the `feedId` attributed to a given transit 
dataset by debugging a graph, this method doesn't allow for proper configuration of real-time feeds 
beforehand. 

Unless OTP manages a single dataset, this method should be avoided at all costs if one wants to link 
real-time feeds with static data.