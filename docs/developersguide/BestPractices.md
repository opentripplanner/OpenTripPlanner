# WIP This is not done or thought trough

# Best Practices
TODO

## General 
  1. [Target low coupling and high cohesion](TODO)
  1. [Module assembly - Dependency Injection in OTP](ModulesAssembly.md)


## Best Practices - Components architecture 
You will find a detailed description of these practises on the guice wiki: https://github.com/google/guice/wiki 

  1. [Minimize mutability](https://github.com/google/guice/wiki/MinimizeMutability)
  1. [Inject only direct dependencies](https://github.com/google/guice/wiki/InjectOnlyDirectDependencies)
  1. [Avoid cyclic dependencies](https://github.com/google/guice/wiki/CyclicDependencies)
  1. [Avoid static state](https://github.com/google/guice/wiki/AvoidStaticState)
  1. [Modules should be fast and side-effect free](https://github.com/google/guice/wiki/ModulesShouldBeFastAndSideEffectFree)
  1. [Keep constructors as hidden as possible](https://github.com/google/guice/wiki/KeepConstructorsHidden)

// TODO - The following lines is not so relevant for OTP - drop these?
  - Use @Nullable - (TGR: I am in favor of this, but it is not used in OTP)
  - Be careful about I/O in Providers
  - Avoid conditional logic in modules
  - Avoid binding Closable resources - (TGR: Kind of obvious)


// TODO - Go through this source an pick what is the most relevant:

 - Effective Java 3rd edition
 - [Pragmatic Programmer](https://www.nceclusters.no/globalassets/filer/nce/diverse/the-pragmatic-programmer.pdf) 


## JavaDoc
TODO 
 - package-info.java
 - class
 - fields
 - methods


