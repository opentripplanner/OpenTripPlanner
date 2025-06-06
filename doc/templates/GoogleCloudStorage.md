# Google Cloud Storage - Using GCS Bucket as a OTP Data Source

## Contact Info

- Thomas Gran, Entur, Norway

## Changelog

### OTP 2.0

- Initial implementation to access Google Cloud Storage (read and write). (December 2019)

## Documentation

To enable this turn on the feature `GoogleCloudStorage`. OTP can load or store artifacts from one or
more Google Cloud Storge locations. Each artifact must be configured in the _build-config.json_:
See [`BuildConfig`](https://github.com/opentripplanner/OpenTripPlanner/blob/dev-2.x/application/src/main/java/org/opentripplanner/standalone/config/BuildConfig.java)
on how to configure artifacts.

<!-- INSERT: config -->
