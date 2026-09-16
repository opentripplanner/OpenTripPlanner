# Google Cloud Storage - Using GCS Bucket as a OTP Data Source

## Contact Info

- Thomas Gran, Entur, Norway

## Changelog

### OTP 2.0

- Initial implementation to access Google Cloud Storage (read and write). (December 2019)

### OTP 2.11

- Large objects are downloaded as several byte-ranges in parallel, which removes the long tail
  caused by the varying throughput of a single connection. (September 2026)

## Documentation

To enable this turn on the feature `GoogleCloudStorage`. OTP can load or store artifacts from one or
more Google Cloud Storge locations. Each artifact must be configured in the _build-config.json_: See
[`BuildConfig`](https://github.com/opentripplanner/OpenTripPlanner/blob/dev-2.x/application/src/main/java/org/opentripplanner/standalone/config/BuildConfig.java)
on how to configure artifacts.

### Reading large objects

Objects above `parallelReadMinSizeMb` are downloaded as several byte-ranges at a time rather than
over a single connection. This matters most for `graph.obj`, which is read on every startup: a
single connection delivers well below the available bandwidth and its throughput varies a lot from
one connection to the next, so the slowest reads take several times the median.

The buffers used for this are allocated outside the Java heap. One object read uses at most
`3 * parallelReads * parallelReadBlockSizeMb` megabytes, which is 192 MB with the default settings.
That memory is not part of the heap, but it does count towards the memory limit of a container, so
lower `parallelReads` or `parallelReadBlockSizeMb` if the process runs close to that limit. Setting
`parallelReads` to `0` restores the previous behaviour of a single sequential stream.

<!-- config BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

### Example configuration

```JSON
// build-config.json
{
  "gsConfig" : {
    "cloudServiceHost" : "http://fake-gcp:4443",
    "credentialFile" : "/path/to/file",
    "parallelReads" : 4,
    "parallelReadBlockSizeMb" : 16,
    "parallelReadMinSizeMb" : 32
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

| Config Parameter                                             |    Type   | Summary                                                                            |  Req./Opt. | Default Value | Since |
|--------------------------------------------------------------|:---------:|------------------------------------------------------------------------------------|:----------:|---------------|:-----:|
| [cloudServiceHost](#gsConfig_cloudServiceHost)               |  `string` | Host of the Google Cloud Storage Server                                            | *Optional* |               |  2.8  |
| [credentialFile](#gsConfig_credentialFile)                   |  `string` | Local file system path to Google Cloud Platform service accounts credentials file. | *Optional* |               |  2.8  |
| [parallelReadBlockSizeMb](#gsConfig_parallelReadBlockSizeMb) | `integer` | The size of each range request in megabytes, when reading in parallel.             | *Optional* | `16`          |  2.11 |
| [parallelReadMinSizeMb](#gsConfig_parallelReadMinSizeMb)     | `integer` | Objects smaller than this are read with a single sequential stream.                | *Optional* | `32`          |  2.11 |
| [parallelReads](#gsConfig_parallelReads)                     | `integer` | Number of byte-ranges of a single object to download concurrently.                 | *Optional* | `4`           |  2.11 |


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


<h4 id="gsConfig_parallelReadBlockSizeMb">parallelReadBlockSizeMb</h4>

**Since version:** `2.11` ∙ **Type:** `integer` ∙ **Cardinality:** `Optional` ∙ **Default value:** `16`   
**Path:** /gsConfig 

The size of each range request in megabytes, when reading in parallel.

Smaller blocks spread a slow connection over a smaller share of the object, but cost one
more request each. The default is a reasonable balance for objects of a few gigabytes.


<h4 id="gsConfig_parallelReadMinSizeMb">parallelReadMinSizeMb</h4>

**Since version:** `2.11` ∙ **Type:** `integer` ∙ **Cardinality:** `Optional` ∙ **Default value:** `32`   
**Path:** /gsConfig 

Objects smaller than this are read with a single sequential stream.

Parallel reads pay for themselves only on large objects. Below this size OTP reads the
object exactly as it did before, allocating no buffers and starting no threads.


<h4 id="gsConfig_parallelReads">parallelReads</h4>

**Since version:** `2.11` ∙ **Type:** `integer` ∙ **Cardinality:** `Optional` ∙ **Default value:** `4`   
**Path:** /gsConfig 

Number of byte-ranges of a single object to download concurrently.

A single Cloud Storage connection tops out well below the available bandwidth, and its
throughput varies substantially from one connection to the next. Reading a large object -
typically `graph.obj` - over one stream therefore has a long tail, where an unlucky
connection takes several times the median. Downloading several ranges at once removes that
tail, and lets the transfer overlap the work of the consumer.

Set to `0` to disable this and use a single sequential stream. Set to `1` to keep a single
stream but move it to a background thread, so the download still overlaps the consumer.

Each concurrent read holds buffers off the Java heap. The memory used by one object read is
at most `3 * parallelReads * parallelReadBlockSizeMb` megabytes - 192 MB with the default
settings. That memory is outside the heap but inside the container memory limit, so reduce
`parallelReads` or `parallelReadBlockSizeMb` if the process is close to that limit.





<!-- config END -->
