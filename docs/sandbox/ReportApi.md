# Report API

The report API is a collection of reports generated as CSV files. The main use-case is to download
data for manual analyzes and verification. The CSV files should not be used as a service by another
programs, the report can be changed at any time - without any notice.

Feel free to add more reports and to add your organization to the contact info list.

## Contact Info

- Entur, Norway
- [Leonard Ehrenfried](https://leonard.io), Germany, [mail@leonard.io](mailto:mail@leonard.io)

## Changelog

- 2021-05-19: Initial version of the report API. Support listing all transfers as a CSV text file.
- 2021-07-19: Add report that exports the bicycle safety factors as CSV and an interactive HTML
  table view.

## Documentation

This module mounts an endpoint for generating reports under `otp/report`. Available reports:

- [/otp/report/transfers.csv](http://localhost:8080/otp/report/transfers.csv)
- [/otp/report/graph.json](http://localhost:8080/otp/report/graph.json)
  Detailed numbers of transit and street entities in the graph
- [/otp/report/bicycle-safety.html](http://localhost:8080/otp/report/bicycle-safety.html):
  Interactive viewer of the rules that determine how bicycle safety factors are calculated.
- [/otp/report/bicycle-safety.csv](http://localhost:8080/otp/report/bicycle-safety.csv): Raw CSV
  data for the bicycle safety report.
    - [Norwegian version](http://localhost:8080/otp/report/bicycle-safety.csv?osmWayPropertySet=norway)
    - [German version](http://localhost:8080/otp/report/bicycle-safety.csv?osmWayPropertySet=germany)
    - [UK version](http://localhost:8080/otp/report/bicycle-safety.csv?osmWayPropertySet=uk)
    - [Finnish version](http://localhost:8080/otp/report/bicycle-safety.csv?osmWayPropertySet=finland)
- [/otp/report/transit/group/priorities](http://localhost:8080/otp/report/transit/group/priorities):
  List all transit groups used for transit-group-priority (Competition neutral planning).   

### Configuration

The report API is turned _off_ by default. To turn it on enable the `ReportApi` feature.

```json
// otp-config.json
{
  "otpFeatures": {
    "ReportApi": true
  }
}
```
