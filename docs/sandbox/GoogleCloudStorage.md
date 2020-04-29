# Google Cloud Storage - Using GCS Bucket as a OTP Data Source 

## Contact Info

- Thomas Gran, Entur, Norway


## Changelog

### OTP 2.0
- Initial implementation of Google Cloud Storage

## Documentation
To enable this turn on `OTPFeature`. Each artifact to load or save to the cloud must be 
configured in build-config.json. See `StorageParameters` on how to configure artifacts.

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

