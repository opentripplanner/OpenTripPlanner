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
[build configuration documentation](BuildConfiguration.md) to find out what the new schema looks like.

By default, OTP starts up even when unknown configuration parameters have been found. This is there
to support the style deployment where old config parameters remain in the file for a certain migration 
period.  

This allows you to roll back the OTP version without the need to roll back the OTP configuration. 

An example: you change the location of the graphs, a critical error occurs afterwards and you need to 
roll back. Any member of the dev-ops team should then be confident that they can roll back without 
risk - even if the environment changed.

### Aborting on invalid configuration

If you want OTP to abort the startup when encountering unknown configuration parameters, you can add 
the flag `--configCheck` to your regular OTP CLI commands.

This should of course be used wisely as it can also accidentally make your production instances refuse 
to start up.
Therefore, we recommend that you use it only in a separate pre-production step, perhaps during graph
build.