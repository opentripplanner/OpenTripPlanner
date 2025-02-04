# System Requirements and Suggestions

## System requirements

### Memory

OTP is relatively memory-hungry as it includes all the required data in memory. How much memory is required to build the graph for OTP, or to run the OTP server, depends on the used data sets (is OSM, elevation and/or transit data included?), on the size of the covered geographical area and the density of the transit and street network. The required memory can vary from less than one GB to more than 100 GB. For example, including all available data for Finland takes a bit over 10 GB but for Germany it requires 95 GB.

### Processor

Single thread performance is an important factor for OTP's performance. Additionally, OTP benefits from larger CPU cache as reading from memory can be a bottleneck.

OTP's performance scales with the number of available CPU cores. OTP processes each request in a separate thread and usually one request doesn't utilize more than one thread, but with some requests and configurations, it's possible that multiple threads are used in parallel for a small part of a request or to process multiple queries within one request. How much parallel processing we utilize in requests might change in the future. Real-time updates also run in a separate thread. Therefore, to have a good performance, it makes sense to have multiple cores available. How OTP uses parallel processing also depends on the available cores (<= 2 cores vs >2 cores) in some cases. Therefore, load testing should be done against a machine that doesn't differ too much from production machines.

Entur and the Digitransit project have found that the 3rd generation AMD processors have a slightly better performance for OTP2 than the Intel 3rd generation CPUs (and especially better than the 2nd generation CPUs).

## Suggested VM types in cloud service providers

### Azure

For Azure, the Digitransit project did benchmarking of the available virtual machines types for OTP 2.3 in early 2023 and found that the `D2as – D96as v5` family had the best performance of the reasonable priced virtual machines types. These machines use the 3rd generation AMD EPYCTM 7763v (Milan) processor. The 3rd generation Intel machines had a slightly worse performance and a slightly higher cost. Digitransit chose to use the `D8as v5` machine as it had enough memory for running OTP in Finland and a reasonable number of vCPUs.

### Google Cloud

Entur uses a scalable fleet of instances of type `c4-standard-8`.
  