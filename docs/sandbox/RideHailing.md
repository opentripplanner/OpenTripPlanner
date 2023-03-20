# Ride hailing services

This sandbox feature allows you to use ride hailing services like Uber.

## Contact Info

- Leonard Ehrenfried, [mail@leonard.io](mailto:mail@leonard.io)

## Configuration

In order enable it add an entry to the `rideHailingServices` section in `router-config.json`.

The individual providers are listed below:

## Uber

<!-- uber-car-hailing BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

| Config Parameter             |   Type   | Summary                                                  |  Req./Opt. | Default Value | Since |
|------------------------------|:--------:|----------------------------------------------------------|:----------:|---------------|:-----:|
| type = "UBER_CAR_HAILING"    |  `enum`  | The type of the service.                                 | *Required* |               |  2.3  |
| clientId                     | `string` | OAuth client id to access the API.                       | *Required* |               |  2.3  |
| clientSecret                 | `string` | OAuth client secret to access the API.                   | *Required* |               |  2.3  |
| wheelchairAccessibleRideType | `string` | The id of the requested wheelchair accessible ride type. | *Required* |               |  2.3  |


##### Example configuration

```JSON
// router-config.json
{
  "rideHailingServices" : [
    {
      "type" : "uber-car-hailing",
      "clientId" : "secret-id",
      "clientSecret" : "very-secret",
      "wheelchairAccessibleRideType" : "car"
    }
  ]
}
```

<!-- uber-car-hailing END -->
