While OTP's GraphQL APIs are very, very stable even across versions, the JSON configuration schema
is not. Changes to it are relatively frequent and you can see all PRs that change it with
the [Github tag 'config change'](https://github.com/opentripplanner/OpenTripPlanner/pulls?q=label%3A%22config+change%22).

### Migrating the JSON configuration

OTP validates the configuration and prints warnings during startup. This means that when you
migrate to a newer version, you should carefully inspect the logs. If you see messages like

```
(NodeAdapter.java:170) Unexpected config parameter: 'routingDefaults.stairsReluctance:1.65' in 'router-config.json'
```

this means there are properties in your configuration that are unknown to OTP and therefore likely
to be a configuration error, perhaps because the schema was changed. In such a case you should
consult the [feature](Configuration.md#otp-features), [router](RouterConfiguration.md) or 
[build configuration documentation](BuildConfiguration.md) to find out what he new schema looks like.

### Aborting on invalid configuration

If you want OTP to abort the startup when encountering invalid configuration, you can add the flag
`--configCheck` to your regular OTP CLI commands.

This should of course be used wisely as it can also accidentally make your production instances refuse 
to start up.
Therefore, we recommend that you use it only in a separate pre-production step, perhaps during graph
build.