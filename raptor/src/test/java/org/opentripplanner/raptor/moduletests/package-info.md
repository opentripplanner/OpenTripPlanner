# Module tests

This package contains functional tests for the Raptor module(`RaptorService`) as a black box. Each
test class should focus on testing ONE feature or scenario. Each test class should contain a small
test data set made with the _feature-under-test_ in mind. This make the tests focused, small and
easier to maintain.

Each test class should document the _feature-under-test_ in the class java-doc.

Each test has a prefix `A01`.. `Z99` to group similar test using the prefix `A`..`Z` and sort each
group from simple to complex tests (`01` to `99`).

## Prefixes

- `A` - Basic tests, testing core features that apply to all journeys
- `B` - On-street/walking access and egress
- `C` - On-street/walking "regular" transfers
- `D` - Transit features, reluctance and preferences
- `E` - Constrained transfers
- `F` - Access and egress with rides (Flex)
- `G` - Access and egress with opening hours/time restrictions
- `H` - Combining the above advanced features
- `I` - Heuristic test
- `J` - Via seach
- `K` - Transit priority
- `L` - Time penalty
 


