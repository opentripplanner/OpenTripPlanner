# Boarding locations

It is often the case that the coordinates of stops are relatively far away from where the passengers
are expected to the wait for the vehicle.

A good example of this is [Buckhead subway station](https://www.openstreetmap.org/way/319512573) in Atlanta.

![Buckhead station](images/buckhead-station.png)

There the coordinates of the stop in GTFS are located near Peachtree Road so OTP would instruct 
passengers to wait on the street rather than walking down the stairs to the platform.

## OSM tagging

We can correct the waiting location for the passenger by adding some tags to OSM which help
OTP decide to correct the location. In general, you should familiarise yourself with the [OSM
wiki page on public transport](https://wiki.openstreetmap.org/wiki/Key:public_transport).

You can add cross-references to a stop's id or code on all OSM entities (nodes, ways, relations) which
have one of the following tag combinations:

- `public_transport=platform`
- `railway=platform`
- `highway=platform`
- `highway=bus_stop`
- `railway=tram_stop`
- `railway=station`
- `railway=halt`
- `amenity=bus_station`
- `amenity=ferry_terminal`

#### Notes

- `public_transport=stop_location` and `railway=stop` are explicitly not on the list as they denote
  the place where the train stops, not the waiting area.
- The `railway` key is deprecated (even though still widespread) and you should use `public_transport` 
  instead.
- OTP does not process isolated nodes, but only nodes, which are part of routable ways. Therefore, adding a single , 
isloated boarding location node to OSM has no effect. There is an exception, though: isolated nodes, which are members of
`public_transport=stop_area` relation, will be considered and automatically linked with the street graph.
For more information, check the [stop area](StopAreas.md) documentation.

## Cross-referencing

In order to tell OTP how to link up the OSM entities to the stops you need to add a `ref` tag whose
value is the stop's id or code. 

However, tagging conventions vary from location to location so other tags can be configured, too.

For example, there is a country-wide stop reference system in Germany called [IFOPT](https://en.wikipedia.org/wiki/en:Identification_of_Fixed_Objects_in_Public_Transport)
and therefore if you want to use it to match stops ([example platform](https://www.openstreetmap.org/way/54224477)), 
add the following to `build-config.json`:

```json
{
  "boardingLocationTags": ["ref", "ref:IFOPT"]
}

```

## Multiple stops on the same platform

Some stations have a middle platform with a stop on either side of it. In such a case, you can 
simply add two or more references separated by a semicolon, as seen in [this example](https://www.openstreetmap.org/way/27558650).

## Related chat threads

- [Thread by Tim Fowle](https://matrix.to/#/!oXNNoHKzbaSOlFzLEt:gitter.im/$740KuVeCc65IW7HO9VjvYk92ACk0cOcjKA_BJhnDMSU?via=gitter.im&via=matrix.org&via=builtin.io)
- [Thread by Sam Cedarbaum](https://matrix.to/#/!oXNNoHKzbaSOlFzLEt:gitter.im/$XY7X9KC0FNSajQ8zDEPUARlv6QHOUd3Qn0R3G2POpqk?via=gitter.im&via=matrix.org&via=builtin.io)