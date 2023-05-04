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

As of May 2023 Uber had the following product IDs in Portland:

  - `6d5eb4b2-3c85-4ef3-854e-3219da0f0df3`: Premier
  - `1196d0dd-423b-4a81-a1d8-615367d3a365`: UberX Share
  - `b6e63411-bf85-4bc7-aca2-bb2e53a20ba4`: Comfort Electric
  - `a6eef2e1-c99a-436f-bde9-fefb9181c0b0`: UberX
  - `62037135-bd5a-43bf-bd77-d4558ffe2bf8`: UberX Priority
  - `0410f2a9-7019-405b-a5ff-d0c92c59339d`: Comfort
  - `a5e722b3-6a20-4c90-b5d7-f9a523904348`: UberXL
  - `4c6e2bde-9242-4634-93f0-8182a4d96e15`: Uber Green
  - `8ddc7ce4-67d1-4ac4-8b56-205bd6a6314e`: Assist
  - `f58761e5-8dd5-4940-a472-872f1236c596`: Uber Pet
  - `c076b1fb-3146-49ec-b56e-eec8348e75bd`: Connect
  - `0e9145be-98bb-48dd-a0bf-32964ac8df19`: WAV




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
