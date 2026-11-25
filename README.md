# CamSys / MnDOT fork of OpenTripPlanner 2

This is CamSys's fork of OpenTripPlanner 2 (based on upstream **2.1.0**). The MnDOT trip planner runs it.
Everything from **"Upstream README"** down is the original upstream text and has not been updated for
this fork.

System context (how OTP fits with the UI, deployer, hub and TDS): see
[`mndot-otp-v2-docker/ARCHITECTURE.md`](https://github.com/camsys/mndot-otp-v2-docker/blob/main/ARCHITECTURE.md).
Fork-specific notes are at the top of [ARCHITECTURE.md](ARCHITECTURE.md).

## Branch strategy

**Trunk plus release tags.** There's one development branch, and environments run released versions.

| Role | Where | Notes |
|---|---|---|
| Development | branch **`v2/dev`** | All work and PRs target `v2/dev`. |
| Releases | tags `otp-<version>-cs`, published to `repo.camsys-apps.com` | Cut by hand from `v2/dev` with `mvn release:prepare release:perform`. There's no Jenkins release job. |
| dev env | the `otp.version` pinned on `mndot-otp-v2-docker` **`main`** (normally a `-SNAPSHOT` of `v2/dev`) | |
| QA / prod env | the `otp.version` pinned on `mndot-otp-v2-docker` **`qa`** / **`prod`** (a release) | |
| Not used | `v2/master`, `v2/release`, `dev-2.x`, `mta-*`, and others | Older or other-client lines. Don't base MnDOT work on them. |

**Look up what each environment runs, and whether the trunk has moved past it:**

```sh
# from a checkout of mndot-otp-v2-docker
for b in main qa prod; do echo "$b: $(git show origin/$b:pom.xml | grep -oE '<otp.version>[^<]+')"; done
# back in this repo: what's on v2/dev that prod doesn't have yet (replace with prod's version)
git fetch --tags && git log --oneline otp-<prod-version>..origin/v2/dev
```

## First-time setup (macOS)

| Tool | Version | How |
|---|---|---|
| JDK | **Amazon Corretto 11** (the images use `amazoncorretto:11`) | `brew install --cask corretto@11`, or unpack the tarball into `~/Library/Java/JavaVirtualMachines` |
| Maven | **3.9.x** | Maven **3.10 fails** on this pom (duplicate `maven-compiler-plugin` / `maven-surefire-plugin` declarations). Use 3.9.x. |
| AWS CLI | v2, read access to `s3://camsys-mndot-otp-graph` | Only needed to download a graph |
| RAM | 16 GB+ | Prod runs `-Xmx10g`; loading the MN graph uses about 6–8 GB |

Dependencies come from Maven Central and CamSys's own repository **`repo.camsys-apps.com`** (public,
anonymous HTTPS read; declared in this pom). No credentials needed.

## Build

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 11)
mvn -DskipTests package          # ~6 min the first time; produces target/otp-<version>-shaded.jar
```

## Run locally (MnDOT graph)

OTP needs a directory containing a built `graph.obj` plus `otp-config.json` and `router-config.json`.

1. **Get a graph** (read-only, ~1.6 GB). A serialized graph only loads into the OTP code line that built
   it, so pick the env whose OTP version matches what you built (prod is rebuilt nightly):
   ```sh
   ENV=v2prod                         # v2dev | v2qa | v2prod
   mkdir -p ~/mndot-otp-data/$ENV
   aws s3 cp --recursive s3://camsys-mndot-otp-graph/$ENV/graph/ ~/mndot-otp-data/$ENV/
   # check: md5 -q graph.obj  ==  first field of graph.obj.md5
   ```
   Or build one yourself; see the mndot-otp-deployer README.
2. **Add the runtime config** for the same environment from `mndot-otp-v2-docker`:
   ```sh
   ENV_BRANCH=prod                    # main | qa | prod, matching the graph
   for f in router-config.json otp-config.json; do
     git -C ../mndot-otp-v2-docker show origin/$ENV_BRANCH:frontend/$f > ~/mndot-otp-data/$ENV/$f
   done
   ```
3. **Start OTP** with the same JVM flags as the containers:
   ```sh
   $JAVA_HOME/bin/java -Xms2g -Xmx10g -Duser.timezone=America/Chicago -XX:+UseParallelGC \
     -jar target/otp-*-shaded.jar --load ~/mndot-otp-data/$ENV
   ```
   It's ready (about 40 s) when the log shows `Grizzly server running.`. Ports: **8080** (HTTP) and
   **8081** (secure). Add `--port N --securePort M` to run a second OTP alongside.
4. **Smoke test** (Downtown Minneapolis → Downtown St Paul):
   ```sh
   curl "http://localhost:8080/otp/routers/default/plan?fromPlace=44.9778,-93.2650&toPlace=44.9537,-93.0900&mode=TRANSIT,WALK&date=$(date -v+1d +%F)&time=8:00am"
   ```
   Expect several itineraries (METRO Green Line, Route 94).

**Harmless log noise:** many `ERROR ... agency time zone differs from graph time zone` lines, and
`ClassNotFoundException instantiating OneBusAway...`. The realtime updaters in `router-config.json` poll the
env's `feedservice-mndot-<env>`. If that's unreachable you get updater warnings, but routing still works.

**IntelliJ:** main class `org.opentripplanner.standalone.OTPMain`, program args `--load <graph dir>`, VM
options as above, JDK = Corretto 11.

## Works with (start order)

| # | Repo | Why |
|---|---|---|
| 1 | **OpenTripPlanner** (this) | Routing API on :8080 |
| 2 | [otp-react-redux](https://github.com/camsys/otp-react-redux) (`camsys/v1/dev`) | The MnDOT UI; points at :8080 |
| 3 | [mobility-hub](https://github.com/camsys/mobility-hub) (optional) | Reads `/otp/routers/default/index/feeds` |
| — | [mndot-otp-deployer](https://github.com/camsys/mndot-otp-deployer) | Builds `graph.obj` with this jar instead of downloading one |

## Local vs the deployed environments

| | Local | Deployed (`mndot-otp-v2-docker` `frontend/` image) |
|---|---|---|
| Code | your checkout | the pinned `otp.version` |
| Graph | whatever you downloaded or built | rebuilt nightly by the deployer, picked up within 5 min |
| UI | separate dev server (:9966) | bundled into the same container (`--clientFiles`), same origin |
| Booking links | depend on `otp-config.json` `"environment"` (see the docker repo README) | same |

---

# Upstream README

## Overview

OpenTripPlanner (OTP) is an open source multi-modal trip planner, focusing on travel by scheduled public transportation in combination with bicycling, walking, and mobility services including bike share and ride hailing. Its server component runs on any platform with a Java virtual machine (including Linux, Mac, and Windows). It exposes REST and GraphQL APIs that can be accessed by various clients including open source Javascript components and native mobile applications. It builds its representation of the transportation network from open data in open standard file formats (primarily GTFS and OpenStreetMap). It applies real-time updates and alerts with immediate visibility to clients, finding itineraries that account for disruptions and service changes.

Note that this branch contains **OpenTripPlanner 2**, the second major version of OTP, which has been under development since Q2 2018 and is approaching release. As of September 2020, OTP2 is in feature freeze and we have tagged a release candidate `v2.0-RC1` which is undergoing testing. 

If you do not want to test or explore this cutting edge version, please switch to the `master` or `dev-1.x` branches for the latest stable 1.x release and upcoming final 1.x release respectively.

## Repository layout

The main Java server code is in `src/main/`. OTP also includes a Javascript client based on the Leaflet mapping library in `src/client/`. This client is now primarily used for testing, with most major deployments building custom clients from reusable components. The Maven build produces a unified ("shaded") JAR file at `target/otp-VERSION.jar` containing all necessary code and dependencies to run OpenTripPlanner.

Additional information and instructions are available in the [main documentation](http://docs.opentripplanner.org/en/dev-2.x/), including a 
[quick introduction](http://docs.opentripplanner.org/en/dev-2.x/Basic-Tutorial/).

## Development 

OpenTripPlanner is a collaborative project incorporating code, translation, and documentation from contributors around the world. We welcome new contributions. Further [development guidelines](http://docs.opentripplanner.org/en/latest/Developers-Guide/) can be found in the documentation.

### Development history

The OpenTripPlanner project was launched by Portland, Oregon's transport agency TriMet (http://trimet.org/) in July of 2009. As of this writing in Q3 2020, it has been in development for over ten years. See the main documentation for an overview of [OTP history](http://docs.opentripplanner.org/en/dev-2.x/History/) and a list of [cities and regions using OTP](http://docs.opentripplanner.org/en/dev-2.x/Deployments/) around the world.


## Mailing Lists

The main forums through which the OpenTripPlanner community organizes development and provides mutual assistance are our two Google discussion groups. Changes and extensions to OTP are debated on the [opentripplanner-dev](https://groups.google.com/forum/#!forum/opentripplanner-dev) developers' list. More general questions and announcements of interest to non-developer OTP users should be directed to the [opentripplanner-users](https://groups.google.com/forum/#!forum/opentripplanner-users) list. Other details of [project governance](http://docs.opentripplanner.org/en/dev-2.x/Governance/) can be found in the main documentation.
