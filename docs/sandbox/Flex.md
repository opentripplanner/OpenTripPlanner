# Flexible transit routing

## Contact Info

- Kyyti Group Oy, Finland
- Entur, Norway
- Hannes Junnila

## Documentation

To enable this turn on `FlexRouting` as a feature in `otp-config.json`.

The GTFS feeds should conform to the 
[GTFS-Flex v2 draft PR](https://github.com/google/transit/pull/388)

## Configuration

This feature allows a limited number of config options. To change the configuration, add the 
following to `router-config.json`.

<!-- config BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

### Example configuration

```JSON
// router-config.json
{
  "flex" : {
    "maxTransferDuration" : "5m",
    "maxFlexTripDuration" : "45m",
    "maxAccessWalkDuration" : "15m",
    "maxEgressWalkDuration" : "15m"
  }
}
```
### Overview

| Config Parameter                                     |    Type    | Summary                                                                                                                       |  Req./Opt. | Default Value | Since |
|------------------------------------------------------|:----------:|-------------------------------------------------------------------------------------------------------------------------------|:----------:|---------------|:-----:|
| [maxAccessWalkDuration](#flex_maxAccessWalkDuration) | `duration` | The maximum duration the passenger will be allowed to walk to reach a flex stop or zone.                                      | *Optional* | `"PT45M"`     |  2.3  |
| [maxEgressWalkDuration](#flex_maxEgressWalkDuration) | `duration` | The maximum duration the passenger will be allowed to walk after leaving the flex vehicle at the final destination.           | *Optional* | `"PT45M"`     |  2.3  |
| [maxFlexTripDuration](#flex_maxFlexTripDuration)     | `duration` | How long can a non-scheduled flex trip at maximum be.                                                                         | *Optional* | `"PT45M"`     |  2.3  |
| [maxTransferDuration](#flex_maxTransferDuration)     | `duration` | How long should a passenger be allowed to walk after getting out of a flex vehicle and transferring to a flex or transit one. | *Optional* | `"PT5M"`      |  2.3  |


### Details

<h4 id="flex_maxAccessWalkDuration">maxAccessWalkDuration</h4>

**Since version:** `2.3` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"PT45M"`   
**Path:** /flex 

The maximum duration the passenger will be allowed to walk to reach a flex stop or zone.

If you have multiple overlapping flex zones the high default value can lead to performance problems.
A lower value means faster routing.

Depending on your service this might be what you want to do anyway: many flex services are used
by passengers with mobility problems so offering a long walk might be problematic. In other words,
if you can walk 45 minutes to a flex stop/zone you're unlikely to be the target audience for those
services.


<h4 id="flex_maxEgressWalkDuration">maxEgressWalkDuration</h4>

**Since version:** `2.3` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"PT45M"`   
**Path:** /flex 

The maximum duration the passenger will be allowed to walk after leaving the flex vehicle at the final destination.

If you have multiple overlapping flex zones the high default value can lead to performance problems.
A lower value means faster routing.

Depending on your service this might be what you want to do anyway: many flex services are used
by passengers with mobility problems so offering a long walk might be problematic. In other words,
if you can walk 45 minutes to a flex stop/zone you're unlikely to be the target audience for those
services.


<h4 id="flex_maxFlexTripDuration">maxFlexTripDuration</h4>

**Since version:** `2.3` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"PT45M"`   
**Path:** /flex 

How long can a non-scheduled flex trip at maximum be.

This is used for all trips which are of type `UnscheduledTrip`. The value includes the access/egress duration to the boarding/alighting of the flex trip, as well as the connection to the transit stop.

<h4 id="flex_maxTransferDuration">maxTransferDuration</h4>

**Since version:** `2.3` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"PT5M"`   
**Path:** /flex 

How long should a passenger be allowed to walk after getting out of a flex vehicle and transferring to a flex or transit one.

This was mainly introduced to improve performance which is also the reason for not
using the existing value with the same name: fixed schedule transfers are computed
during the graph build but flex ones are calculated at request time and are more
sensitive to slowdown.

A lower value means that the routing is faster.





<!-- config END -->

## Changelog

### OTP 2.1

- Initial implementation of Flexible transit routing
- Use one-to-many search in order to make the performance of the StreetFlexPathCalculator
  acceptable. (April 2021)
- Also link transit stops used by flex trips to the closest car traversable edge. This allows flex
  street routing all the way to the stop. (April 2021)
- Fix performance issues with the
  StreetFlexPathCalculator [#3460](https://github.com/opentripplanner/OpenTripPlanner/pull/3460)
- Improve performance of flex access/egress
  routing [#3661](https://github.com/opentripplanner/OpenTripPlanner/pull/3661)
- Allow getting on and off at the same flex stop
  time [#3720](https://github.com/opentripplanner/OpenTripPlanner/pull/3720)
- Calculate fare for flex
  routes [#3743](https://github.com/opentripplanner/OpenTripPlanner/pull/3743)

### OTP 2.3
- Enable configuration of `maxFlexTripDuration` and change of type of `maxTransferDuration`
  routes [#4642](https://github.com/opentripplanner/OpenTripPlanner/pull/4642)
