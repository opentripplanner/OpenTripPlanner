# Service package

This package contains small services usually specific to one or a few use-cases. In contrast
to a domain model they may use one or many domain models and other services, as long as there
are no cyclic dependencies. 

The typical package structure of the `<name>` service:

```
o.o.service.<name>
    configure                   -- Dependency Injection configuration
      <Name>Module
    grapbuilder                 -- If this module has its own graph-builder module
      <Name>GraphBuilderModule
    internal                    -- Internal implementaion 
      Default<Name>Service
      Default<Name>Repository
    model                       -- Public model/api 
      <Domain Model Classes>
    <Name>Service               -- Interface for the (read-only) service
    <Name>Repository            -- Optional interface for updating the model (in memory)
```

 - The `grapbuilder` is just an example, in case this service has its own 
   graph-builder module.
 - The `internal` package can be split into sub-packages, if needed. 
 - The aggregate root `<Name>Service` is defined in the root package.
 - The `<Name>Repository` interface is only needed if the repository 
   is used outside the module by an updater or another service.
 - Both the `Default<Name>Repository` and `Default<Name>Service` should be 
   thread safe.
 - The `Default<Name>Repository` should be serialized in the `graph.obj` file and hence
   needs to be `Serializable`.

The `worldenvelope` service is used to illustrate the above example; Hence the `worldenvelope`
has a `WorldEnvelopeRepository` even if it is not required.