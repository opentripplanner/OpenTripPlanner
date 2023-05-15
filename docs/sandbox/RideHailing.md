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

| Config Parameter                                                                      |    Type    | Summary                                                             |  Req./Opt. | Default Value | Since |
|---------------------------------------------------------------------------------------|:----------:|---------------------------------------------------------------------|:----------:|---------------|:-----:|
| type = "uber-car-hailing"                                                             |   `enum`   | The type of the service.                                            | *Required* |               |  2.3  |
| clientId                                                                              |  `string`  | OAuth client id to access the API.                                  | *Required* |               |  2.3  |
| clientSecret                                                                          |  `string`  | OAuth client secret to access the API.                              | *Required* |               |  2.3  |
| [wheelchairAccessibleProductId](#rideHailingServices_0_wheelchairAccessibleProductId) |  `string`  | The id of the requested wheelchair-accessible product ID.           | *Required* |               |  2.3  |
| [bannedProductIds](#rideHailingServices_0_bannedProductIds)                           | `string[]` | The IDs of those product ids that should not be used for estimates. | *Optional* |               |  2.3  |


#### Details

<h4 id="rideHailingServices_0_wheelchairAccessibleProductId">wheelchairAccessibleProductId</h4>

**Since version:** `2.3` ∙ **Type:** `string` ∙ **Cardinality:** `Required`   
**Path:** /rideHailingServices/[0] 

The id of the requested wheelchair-accessible product ID.

See `bannedProductIds` for a list of product IDs.

<h4 id="rideHailingServices_0_bannedProductIds">bannedProductIds</h4>

**Since version:** `2.3` ∙ **Type:** `string[]` ∙ **Cardinality:** `Optional`   
**Path:** /rideHailingServices/[0] 

The IDs of those product ids that should not be used for estimates.

See the current [list of Uber product ids](https://gist.github.com/leonardehrenfried/70f1346b045ad58224a6f43e4ef9ce7c).




##### Example configuration

```JSON
// router-config.json
{
  "rideHailingServices" : [
    {
      "type" : "uber-car-hailing",
      "clientId" : "secret-id",
      "clientSecret" : "very-secret",
      "wheelchairAccessibleProductId" : "545de0c4-659f-49c6-be65-0d5e448dffd5",
      "bannedProductIds" : [
        "1196d0dd-423b-4a81-a1d8-615367d3a365",
        "f58761e5-8dd5-4940-a472-872f1236c596"
      ]
    }
  ]
}
```

<!-- uber-car-hailing END -->
