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

<!-- INSERT: config -->
