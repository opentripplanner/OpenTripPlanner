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

<!-- config BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

### Example configuration

```JSON
// build-config.json
{
  "gsConfig" : {
    "cloudServiceHost" : "http://fake-gcp:4443",
    "credentialFile" : "/path/to/file"
  },
  "graph" : "gs://otp-test-bucket/a/b/graph.obj",
  "buildReportDir" : "gs://otp-test-bucket/a/b/np-report",
  "osm" : [
    {
      "source" : "gs://otp-test-bucket/a/b/northpole.pbf"
    }
  ],
  "dem" : [
    {
      "source" : "gs://otp-test-bucket/a/b/northpole.dem.tif"
    }
  ],
  "transitFeeds" : [
    {
      "type" : "gtfs",
      "source" : "gs://otp-test-bucket/a/b/gtfs.zip"
    }
  ]
}
```
### Overview

| Config Parameter                               |   Type   | Summary                                                                            |  Req./Opt. | Default Value | Since |
|------------------------------------------------|:--------:|------------------------------------------------------------------------------------|:----------:|---------------|:-----:|
| [cloudServiceHost](#gsConfig_cloudServiceHost) | `string` | Host of the Google Cloud Storage Server                                            | *Optional* |               |  2.8  |
| [credentialFile](#gsConfig_credentialFile)     | `string` | Local file system path to Google Cloud Platform service accounts credentials file. | *Optional* |               |  2.8  |


### Details

<h4 id="gsConfig_cloudServiceHost">cloudServiceHost</h4>

**Since version:** `2.8` ∙ **Type:** `string` ∙ **Cardinality:** `Optional`   
**Path:** /gsConfig 

Host of the Google Cloud Storage Server

Host of the Google Cloud Storage server. In case of a real GCS Bucket this parameter can be
omitted. When the host differs from the usual GCS host, for example when emulating GCS in a
docker container for testing purposes, the host has to be specified including the port.
Eg: http://localhost:4443

<h4 id="gsConfig_credentialFile">credentialFile</h4>

**Since version:** `2.8` ∙ **Type:** `string` ∙ **Cardinality:** `Optional`   
**Path:** /gsConfig 

Local file system path to Google Cloud Platform service accounts credentials file.

The credentials are used to access GCS URLs. When using GCS from outside of Google Cloud you
need to provide a path the the service credentials. Environment variables in the path are
resolved.

This is a path to a file on the local file system, not an URI.





<!-- config END -->
