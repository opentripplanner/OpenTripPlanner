# TRIAS API

## Contact Info

- Leonard Ehrenfried, mail@leonard.io

## Documentation

This sandbox feature implements part of the [TRIAS API](https://www.vdv.de/projekt-ip-kom-oev-ekap.aspx) 
which is a standard defined by the German VDV, the association of transit agencies.

The following request types are supported:

- `StopEventRequest`

To enable this turn on `TriasApi` as a feature in `otp-config.json`.

### URLs

- Endpoint: `http://localhost:8080/otp/trias/v1/`
- Visual API Explorer: `http://localhost:8080/otp/trias/v1/explorer`

## Configuration

This feature allows a small number of config options. To change the configuration, add the 
following to `router-config.json`.

<!-- INSERT: config -->
