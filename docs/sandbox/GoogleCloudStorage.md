# Google Cloud Storage - Using GCS Bucket as a OTP Data Source 

## Contact Info

- Thomas Gran, Entur, Norway


## Changelog

### OTP 2.0
- Initial implementation to access Google Cloud Storage (read and write). (December 2019)

## Documentation
To enable this turn on the feature `GoogleCloudStorage`. OTP can load or store artifacts from one or more Google Cloud Storge locations. Each artifact must be configured in the _build-config.json_: See [`StorageConfig`](https://github.com/opentripplanner/OpenTripPlanner/blob/v2.0.0/src/main/java/org/opentripplanner/standalone/config/StorageConfig.java) on how to configure artifacts.



Example (build-config.json):
```json
{
    :
    storage : {
        gcsCredentials: "/Users/alf/secret/otp-test-1234567890.json",
        osm : [ "gs://otp-test-bucket/a/b/northpole.pbf" ], 
        dem : [ "gs://otp-test-bucket/a/b/northpole.dem.tif" ],
        gtfs: [ "gs://otp-test-bucket/a/b/gtfs.zip" ],
        graph: "gs://otp-test-bucket/a/b/graph.obj"
        buildReportDir: "gs://otp-test-bucket/a/b/np-report"
    }
}
```

