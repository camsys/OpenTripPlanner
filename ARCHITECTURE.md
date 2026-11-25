# CamSys / MnDOT fork: architecture notes

> **Pre-existing file.** The sections from "OTP Architecture" down are **upstream OpenTripPlanner's**
> architecture document, kept unchanged. This top section was added for the CamSys/MnDOT fork (MN-168).
> System-level architecture (deployer, UI, hub, TDS, AWS, promotion): see
> [`mndot-otp-v2-docker/ARCHITECTURE.md`](https://github.com/camsys/mndot-otp-v2-docker/blob/main/ARCHITECTURE.md).

## Where this fork differs from upstream 2.1.0

Compare with `git log --oneline v2.1.0..origin/v2/dev` (several hundred commits). The MnDOT-relevant ones:

| Area | What | Where |
|---|---|---|
| Flex / demand-response booking | Each flex leg gets a `bookingUrl` pointing at the TDS booking portal, with pickup/dropoff address (reverse-geocoded), time, agency and `tripId` as query params. The base URL is chosen by `otp-config.json` `"environment"`: `DEV` → `booking.dev.mndot…`, `QA` → `booking.qa.mndot…`. **Any other value, or none, logs "No base booking URL found" and no booking link is generated.** Which value each environment sets is in the per-branch table of the mndot-otp-v2-docker README. | `api/resource/TripPlannerResponse.java`, `api/model/ApiLeg.bookingUrl`, `model/ContactInfo` |
| Trip validation for booking | `GET /otp/routers/default/validate/{date}/{tripId}`, called by the TDS booking frontend before a booking | `api/resource/PlannerResource.java` |
| Routing fixes | MNDOT-224 (don't return indirect trips when a direct trip exists), MN-210, flex departure-time and service-day fixes (several reverted in 2024-10) | `routing/algorithm/...` |
| Fares v2, feed-id handling, loop trips | Various CamSys features shared with other clients (MTA, NYS) | `ext/`, `model/`, `graph_builder/` |

## Runtime shape in MnDOT

- **Entry point:** `org.opentripplanner.standalone.OTPMain`, run as `--load <graphDir>` in prod (plus
  `--clientFiles <ui dist> --clientPath / --disableNativeClient` to serve the React UI).
- **Graph:** built offline by `mndot-otp-deployer` (`--build --save`), stored as `graph.obj` in
  `s3://camsys-mndot-otp-graph/<env>/graph/`. It's a Java-serialized graph, so the loading OTP
  must be the same code line that built it.
- **Config:** `otp-config.json` (feature flags `FlexRouting`, `SandboxAPILegacyGraphQLApi`,
  `flexMaxTripDurationInHours`, `environment`) and `router-config.json` (realtime updaters
  pointing at `feedservice-mndot-<env>`), both from `mndot-otp-v2-docker/frontend/`.
- **APIs used by MnDOT clients:** REST `/otp/routers/default/plan` (UI), `/index/feeds` (mobility-hub),
  `/validate/...` (TDS frontend), legacy GraphQL (sandbox).

---

# OTP Architecture
OTP is developed over more than 10 years, and most of the design documentation is in the code 
as comments and JavaDoc. Over the years the complexity have increased, and the natural developer 
turnover creates a demand for more architecture and design documentation. The new OTP2 
documentation is put together with the source; hopefully making it easier to maintain. Instead of
documenting modules in old style _package-info.java_ files we use _package.md_ files. This document
should serve as an index to all existing top-level documented components.

This document is far from complete - hopefully it can evolve over time and become a good 
introduction to OTP architecture. The OTP project GitHub issues are a good place to look for detailed 
discussions on many design decisions.   

Be sure to also read the [developer documentation](docs/Developers-Guide.md). 
 
## Modules/Components
Below is a list of documented components in OTP. Not every component is documented at a high level,
but this is a start and we would like to expand this list in the future.

### [OTP Configuration design](src/main/java/org/opentripplanner/standalone/config/package.md)

The Configuration module is responsible for loading and parsing OTP configuration files and map 
them into Plan Old Java Objects (POJOs). These POJOs are injected into the other components.


### [REST API](src/main/java/org/opentripplanner/api/package.md)

Short introduction to the REST API.


### [GTFS import module](src/main/java/org/opentripplanner/gtfs/package.md)

Used to import GTFS transit data files.


### [NeTEx import module](src/main/java/org/opentripplanner/netex/package.md)

Used to import NeTEx transit data files.

### Transit Routing

#### [Raptor transit routing](src/main/java/org/opentripplanner/transit/raptor/package.md)

This is the OTP2 new transit routing engine implemented using the Raptor algorithm. It explains how
Raptor works, the important concepts and the design. It might be worth reading even if you are not
a developer - just to understand how the transit routing works. 

The Raptor functionality is quite complex, so we want to isolate it from the remaining code. Therefore, the 
raptor component is designed to have as few dependencies as possible. In fact there are _no_ 
dependencies from Raptor to other parts of OTP code, only to utility classes not found in the JDK.
Also, the code follows a stricter object-oriented design, than most other parts of OTP. The Raptor 
implementation is highly critical code, hence we set the bar higher with respect to code quality.
 
OTP provides transit data to Raptor by implementing the _raptor/api/transit_ model. The 
[RoutingService](src/main/java/org/opentripplanner/routing/RoutingService.java)
is responsible for mapping from the OTP context to a 
[RaptorRequest](src/main/java/org/opentripplanner/transit/raptor/api/request/RaptorRequest.java)
and then map the result, [Raptor Path](src/main/java/org/opentripplanner/transit/raptor/api/path/Path.java),
back to the OTP internal domain. This might seem like a lot of unnecessary mapping, but 
mapping is simple - routing is not. 

The performance of Raptor is important, and we care about every millisecond. All changes to the
existing Raptor coded should be tested with the 
[SpeedTest](src/test/java/org/opentripplanner/transit/raptor/speed_test/package.md) and 
compared with an earlier version of the code to make sure the performance is NOT degraded. 

#### [Transfer path optimization](src/main/java/org/opentripplanner/routing/algorithm/transferoptimization/package.md)
Describes the transfer functionality, the design and the implementation. The logic for finding the best
transfer is distributed to the Raptor and the [OptimizeTransferService](src/main/java/org/opentripplanner/routing/algorithm/transferoptimization/OptimizeTransferService.java).
