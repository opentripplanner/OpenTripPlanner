# Ride hailing services

This sandbox feature allows you to use ride hailing services like Uber.

## Contact Info

- Leonard Ehrenfried, [mail@leonard.io](mailto:mail@leonard.io)

## Configuration

In order enable this feature, add a new section `rideHailingServices` in `router-config.json`.

The supported ride-hailing providers are listed below.

### Uber

<!-- uber-car-hailing BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

| Config Parameter                                          |    Type    | Summary                                                            |  Req./Opt. | Default Value | Since |
|-----------------------------------------------------------|:----------:|--------------------------------------------------------------------|:----------:|---------------|:-----:|
| type = "uber-car-hailing"                                 |   `enum`   | The type of the service.                                           | *Required* |               |  2.3  |
| clientId                                                  |  `string`  | OAuth client id to access the API.                                 | *Required* |               |  2.3  |
| clientSecret                                              |  `string`  | OAuth client secret to access the API.                             | *Required* |               |  2.3  |
| wheelchairAccessibleRideType                              |  `string`  | The id of the requested wheelchair accessible ride type.           | *Required* |               |  2.3  |
| [bannedRideTypes](#rideHailingServices_0_bannedRideTypes) | `string[]` | The IDs of those ride types that should not be used for estimates. | *Optional* |               |  2.3  |


#### Details

<h4 id="rideHailingServices_0_bannedRideTypes">bannedRideTypes</h4>

**Since version:** `2.3` ∙ **Type:** `string[]` ∙ **Cardinality:** `Optional`   
**Path:** /rideHailingServices/[0] 

The IDs of those ride types that should not be used for estimates.



##### Example configuration

```JSON
// router-config.json
{
  "rideHailingServices" : [
    {
      "type" : "uber-car-hailing",
      "clientId" : "secret-id",
      "clientSecret" : "very-secret",
      "wheelchairAccessibleRideType" : "car",
      "bannedRideTypes" : [
        "type1",
        "type2",
        "type3"
      ]
    }
  ]
}
```

<!-- uber-car-hailing END -->
