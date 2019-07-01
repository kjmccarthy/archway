# Heimdali

## Running Locally

_NOTE_ Make sure you have `npm` and IntelliJ installed.

### Configuration

In order to run the API locally, you'll need to prepare your development environment.

This will download the integration test repository which contains credentials and place them in `itest-config`, as well
as softlink the application config to the API resources so the application can be run locally.

```bash
$ make itest-init
```

### Run The App (API-focused development)

- Open the project in IntelliJ
- Open `com.heimdali.Server` and click the green "play" button next to the `object` definition
- The first run won't work, because we need to change the newly created run configuration
- Open the run configuration drop down and click "Edit Configurations..."
- Select the "Server" configuration and enable "Include dependencies with 'Provided' scope"
- Restart the app
- Open the "Terminal" tab
- Run `npm i` which will install the npm dependencies
- Run `npm start` which will spin up the UI

### Run The App (UI-focused development)

- In the terminal tab, run `./sbt -Djavax.net.ssl.trustStore=<truststore> compile "api/runMain com.heimdali.Server"`
- In another terminal tab, run `npm i` which will install the npm dependencies
- Run `npm start` which will spin up the UI
- Once the UI has started you can start editing and the UI will refresh based on your changes

## Heimdali API

Heimdali API is the REST interface primally enabling functionality to it's UI counterpart but also a means for automation.

### Code

#### Packages

The code is comprised of a few primary packages:

- REST API - `com.heimdali.rest`
  Responsible for serving requests via HTTP.
- Startup - `com.heimdali.startup`
  Responsible for managing initial (and often repeating) tasks.
- Services - `com.heimdali.services`
  Responsible for providing business logic.
- Clients - `com.heimdali.clients`
  Responsible for interacting with third party integrations like CM API.
- Repositories - `com.heimdali.repositories`
  Responsible for managing interactions with the meta database.
- Provisioning - `com.heimdali.provisioning`
  Responsible for applying metadata to the cluster and resources requested.
- Models - `com.heimdali.models`
  Responsible for representing the domain model of the application.

#### Functional Programming/Cats

Some projects are based on AKKA, Play, Spark (in the case of batch), or some other "pattern library." Heimdali is built on Cats, Cats Effect, Http4s, Circe, and Doobie, all in the [Typelevel](http://typelevel.org) stack. These libraries are all built on Cats and encourage functional programming. If you've never used Cats or done functional programming, the good thing is, most patterns are already established and can just be repeated. All of these libraries have excellent documentation, and even better people in Gitter ready and willing to answer questions (just like us on ##heimdali-dev in Slack).

### Database

The metadata for Heimdali is broken up into two main parts: workspace metadata and application configuration.

#### Workspace Metadata

All workspaces can contain a collection of topics, applications, databases, and resource pools. Depending on the template used, a workspace is stored and when applied, timestamps on the related entity are updated to indicate progress.
![](metadata.png)

#### Configuration

![](config.png)

### Integration test package

An integration test jar is included in the parcel at \$PARCELS_ROOT/HEIMDALI/usr/lib/heimdali-api/heimdali-test.jar

Run tests by adding your application.conf to the classpath and choosing a test:

````bash
java -cp "/path/to/application.conf:cloudera-integration/build/HEIMDALI-1.5.1/usr/lib/heimdali-api/*:cloudera-integration/build/HEIMDALI-1.5.1/usr/lib/heimdali-api-tests/*" org.scalatest.tools.Runner -o -R cloudera-integration/build/HEIMDALI-1.5.1/usr/lib/heimdali-api-tests/heimdali-integration-tests.jar -q Spec```bash

When run in a dev environment this looks like:

```bash
java -cp "common/src/test/resources/application.test.conf:integration-test/target/scala-2.12/heimdali-test.jar" org.scalatest.run com.heimdali.clients.LDAPClientImplIntegrationSpec
````

Build the test jar locally with

```bash
make package-tests
```

Create integration tests with `make package-tests`.

### Contributing

Pull requests are welcome. For major changes, please start the discussion on [#heimdali-dev](https://phdata.slack.com/app_redirect?channel=heimdali-dev).

Please make sure to review [CONTRIBUTING.md](CONTRIBUTING.md)

## Heimdali UI

Heimdali UI is a web application for managing resources.

### What's Being Used?

- [React](http://facebook.github.io/react/) for managing the presentation logic of your application.
- [Redux](http://redux.js.org/) + [Redux-Immutable](https://github.com/gajus/redux-immutable/) + [Reselect](https://github.com/reduxjs/reselect/) for generating and managing your state model.
- [Redux-Saga](https://github.com/redux-saga/redux-saga/) for managing application side effects.
- [Antd](https://ant.design/) for ui elements such as sidebar, dropdown, card, etc.
- [Formik](https://github.com/jaredpalmer/formik/) and [Redux-Form](https://redux-form.com/) for handling forms efficiently.
- [Fuse](http://fusejs.io/) for fuzzy-search feature.
- [React-Csv](https://github.com/react-csv/react-csv/) for exporting data in csv format.
- [Lodash](https://lodash.com/) for using various utility functions.
- [Node](https://nodejs.org) Version 8.x
- [NPM](https://npmjs.com) Version 5.x

### File Structure

#### public/

In this folder is a default `index.html` file for serving up the application. Fonts used by application also reside here.

###### images/

Folder containing image assets used in the application.

#### src/

The client folder houses the client application for your project. This is where your client-side Javascript components, logical code blocks and image assets live.

###### components/

Here reside the components that are used globally, such as ListCardToggle, Behavior and WorkspaceListItem.

###### containers/

Here we have containers, the components that are connected to redux. Each container has its own actions, reducers, sagas and selectors. Every routes of the app has their relevant containers. The sub-directory names indicate what route they are pointing. Some containers has subfolder named `components` that includes components used for that specific container.

###### models/

Here we define all the models used for the application, including Workspace and Cluster.

###### redux/

Global reducers and sagas of the application stays here.

###### service/

Here resides the code for making api calls.

## Building a CSD and Parcel locally

Set the parcel/csd version

```
export HEIMDALI_VERSION=1.5.15.6
```

```
make dist
```

## Creating a release

### Versioning

Heimdail follows [Semantic Versioning](https://semver.org/). Given a version like 1.1.1, with the parts called
<major>.<minor>.<patch>

- Major versions will increment if there are breaking API changes
- Minor versions will increment if there is new functionality
- Patch versions will increment if there are fixes to existing functionality

We also add a 'release candidate' version to the end of the semver version so we can test and track versions internally
before they are released to the public, for example `1.1.0-rc1` for release candiate '1'.

### Builds

There are three builds defined in Bitbucket

- default: This build runs on each committed branch, building the UI and API code
- nightly: This build creates a CSD and parcel in the parcels-dev Artifactory Repo nightly
- commit: This build can be run to create a custom build for a specific commit hash. This is useful for on-demand testing

### Promoting a release

After a release candidate has been tested and approved for release the release candidate can be promoted to the `parcels-release`
repository using the `promote-release` script:

```
export ARTIFACTORY_USER=<user>
export ARTIFACTORY_TOKEN=<token>
export SOURCE_VERSION=1.1.0-rc1
export TARGET_VERSION=1.1.0

build-support/bin/promote-release
```

By default `SOURCE_REPO=parcels-def` and `TARGET_REPO=parcels-release`, so you don't need to set these during a normal release workflow.
