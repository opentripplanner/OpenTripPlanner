# Report API

The report API is a collection of reports generated as CSV files. The main use-case is to download
data for manual analyzes and verification. The CSV files should not be used as a service by another
programs, the report can be changed at any time - without any notice.

Feel free to add more reports and to add your organization to the contact info list.


## Contact Info
- Entur, Norway


## Changelog

- Initial version of the report API (Mai 2021). Support listing all transfers as a CSV 
  text file.


## Documentation

This module mount an endpoint for generating reports under `otp/report`. Available reports:

 - [/otp/report/transfers.csv](http://localhost:8080/otp/report/transfers.csv)

 
### Configuration

The report API is turned _off_ by default. To turn it on enable the `ReportApi` feature.
 
 
