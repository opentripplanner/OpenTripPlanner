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

By default, OTP starts up even if unknown configuration parameters exist. This supports forward and backwards
migration of config parameters independent if the OTP version. This allows the configuration to follow 
the lifecycle of the environment and still be able to roll back OTP. 
Combined with the config parameter substitution it also allows using the same configuration in 
different environments. Tip! Rolling out the configuration before rolling out a new
version of OTP, ensure you that you are safe and can roll back later. 

An example: you change the location of the graphs, a critical error occurs afterwards and you need to 
roll back. Any member of the dev-ops team should then be confident that they can roll back without 
risk - even if the environment changed since the last OTP version was rolled out.

### Aborting on invalid configuration

If you want OTP to abort the startup when encountering unknown configuration parameters, you can add 
the flag `--abortOnUnknownConfig` to your regular OTP CLI commands.

This should of course be used wisely as it can also accidentally make your production instances refuse to start up.
For some deployments this is a good solution - especially if the config substitution feature is used to inject
environment specific information. Using this feature in the graph-build phase is less risky, than in the OTP serve phase.