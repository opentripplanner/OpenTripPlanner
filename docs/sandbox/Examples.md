# Statistics API - OTP Sandbox Extension Example

## Contact Info
- Thomas Gran, Entur, Norway

## Changelog

### April 2019 (in progress)

- Initial setup of the first new OTP Sandbox Extension. (April 2019)
- Added a simple GraphQL API for retrieving Graph statistics. (May 2019)
- Moved Graph Example Updaters from main code to sandbox examples. (May 2019) 

## Documentation

### Graph Statistics Resource
This extension show how to create a web endpoint to get some simple statistics: 
- Number of stops in the graph
 
### Graph Example Updaters
There is two example updaters, see the Java doc for more information on how to implement 
new updater. 
- The `ExampleGraphUpdater` class shows an example of how to implement a graph updater suited for 
  streaming updaters.
- The `ExamplePollingGraphUpdater` class shows an example of how to implement a graph updater 
   suited for polling updaters.

