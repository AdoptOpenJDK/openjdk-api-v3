# Contributing to AdoptOpenJDK API (V3)

## Overview

The AdoptOpenJDK API V3 is a Kotlin application (fronted by Swagger and OpenAPI) which makes 
calls via the GitHub API in order to retrieve AdoptOpenJDK binaries and metadata.

Since the GitHub API is rate limited we use MongoDB as a caching mechanism.

## Source code management and branching

We treat the AdoptOpenJDK org repository as the canonical repository for deploying the API from. 
We use the `staging` branch to trial any changes in a Production like environment and then 
eventually merge into `master` for a real Production deployment.

**NOTE** Please ensure for any significant change that you Pull Request to the `staging` branch 

## Build

### Pre-Requisites

Java 11 is a requirement to build the project.

### Optional Set-up

If you want to use the updater tool to add entries into the database, you need to generate a github token, which can be done here: https://github.com/settings/tokens. It doesn't need any specific permissions. Once you have a token generated, create a file at ~/.adopt_api/token.properties and type `token=your-token-here`

The production server uses mongodb to store data, however you can also use Fongo. If you would like to install mongodb and are on mac, I used this guide https://zellwk.com/blog/install-mongodb/ which utilises homebrew. You can also install `mongo` which is a command-line tool that gives you access to your mongodb, allowing you to manually search through the database.

### Build Tool

We use a Maven Wrapper (mvnw) to ensure that there's a consistent, repeatable build. The 
[POM File](./pom.xml) is the place to start.

**NOTE:** We use a multi-module project structure. The root level POM contains a majority 
of the configuration that the children inherit from. 

### Build Command

To perform a full build and test you run the following:

`./mvnw clean install`

If you wish to view all of the Maven reporting about the project you run the following:

`./mvnw clean install site`

### Docker
For convenience, you can build the API components with `Docker` and `docker-compose`. 

```bash
docker-compose build
``` 

Using a multi-stage [Dockerfile](Dockerfile) build, a Docker image is produced that supports running both the updater and the front-end.

The [docker-compose.yml](docker-compose.yml) also defines a service for each component, as well a dependency on MongoDB, allowing you to spin up the full stack required for the API.

```bash
docker-compose up
``` 

You will need to wait the updater to complete its first full run before the API is usable. There is currently no persistence between runs.  

## Testing

**WARN** This API is critical to the success of AdoptOpenJDK therefore it is 
essential that tests are provided for all new functionality. 

### Code Coverage

TBD

## Continuous Integration (CI)

### Pull Requests

There is a [GitHub Action](.github\workflows\build.yml) file which the CI system 
in GitHub uses to build and test a Pull Request.

**NOTE:** Please update the dependencies in this file if you have changed the versions of:
 
* The JDK
* ktlint
* openapi-generator-maven-plugin  

## API Definition and Usage

We use Swagger to document the API. The Swagger documentation can be viewed at: [swagger-ui](https://api.adoptopenjdk.net/swagger-ui). 
The Open API definition for this can be viewed at [openapi](https://api.adoptopenjdk.net/openapi).

## Deployment / Continuous Deployment (CD)

You can choose to deploy this API where you wish, for AdoptOpenJDK we use Continuous Deployment.

### AdoptOpenJDK

For AdoptOpenJDK, this API deploys to Red Hat OpenShift and is front ended by Cloud Flare as a CDN

The Jenkins [AdoptOpenJDK CI Server](https://ci.adoptopenjdk.net) will automatically 
deploy Pull Requests to OpenShift to Staging (the `staging` branch) or Production (the `master` branch.)

## Code Architecture and Code

The AdoptOpenJDK API V3 is a Kotlin application (fronted by Swagger and OpenAPI) which makes 
calls via the GitHub API in order to retrieve AdoptOpenJDK binaries and metadata.

Since the GitHub API is rate limited we use MongoDB as a caching mechanism.

See [Code Structure](./docs/STRUCTURE.md) doc for more details.

## Common Tasks

In this section we list some common tasks and where to start.

### I want support a new version string

If you need to add/edit/remove a supported version string then you need to update the [VersionParser](adoptopenjdk-api-v3-models/src/main/kotlin/net/adoptopenjdk/api/v3/parser/VersionParser.kt) and 
its corresponding [VersionParserTest](adoptopenjdk-api-v3-models/src/test/kotlin/net/adoptopenjdk/api/VersionParserTest.kt).

### I want to add a new variant such as OpenJDK's project amber or 

You'll need to start at the [Platforms JSON](adoptopenjdk-api-v3-frontend/src/main/resources/JSON/platforms.json) and 
[Variants JSON](adoptopenjdk-api-v3-frontend/src/main/resources/JSON/variants.json).
