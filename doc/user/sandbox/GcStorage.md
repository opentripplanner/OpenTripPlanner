# Google Cloud Storage Integration

<!-- config BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

### Example configuration

```JSON
// build-config.json
{
  "gsConfig" : {
    "cloudServiceHost" : "http://fake-gcp:4443",
    "credentialFile" : "/path/to/file"
  }
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

The credentials is used to access GCS urls. When using GCS from outside of Google Cloud you
need to provide a path the the service credentials. Environment variables in the path are
resolved.

This is a path to a file on the local file system, not an URI.





<!-- config END -->