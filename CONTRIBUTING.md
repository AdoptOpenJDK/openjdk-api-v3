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

Java 11 is a requirement to build to project.

### Build Tool

We use a Maven Wrapper (mvnw) to ensure that there's a consistent, repeatable build. The 
[POM File](./pom.xml) is the place to start.

**NOTE:** We use a multi-module project structure. The root level POM contains a majority 
of the configuration that the children inherit from. 

### Build Command

To perform a full build and test you run the following:

`./mvnw clean install`

If you wish to view all fo the Maven reporting about the project you run the following:

`./mvnw clean install site`

## Testing

**WARN** This API is critical to the success of AdoptOpenJDK, so we have a strong preference that 
for any new functionality, you must add tests. 

### Code Coverage

TBD

## Continuous Integration (CI)

### Pull Requests

There is a [Travis YAML](.github\workflows\build.yml) file which the Travis CI 
system in GitHub uses to build and test a Pull Request.

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

### Code Architecture

We split the API into 4 modules:

1. [models](adoptopenjdk-api-v3-models) - The core domain modeling for the API.
2. [persistence](adoptopenjdk-api-v3-persistence) - The layer that interacts with the Mongo DB cache.
3. [updater](adoptopenjdk-api-v3-updater) - The layer that interacts with the GitHub repositories (where we store JDK / JRE binaries). 
4. [frontend](adoptopenjdk-api-v3-frontend) - The layer that responds to requests.

### Models

Contains the domain modeling for the API, including download stats.

Contains the important [VersionParser](adoptopenjdk-api-v3-models/src/main/kotlin/net/adoptopenjdk/api/v3/parser/VersionParser.kt) and 
its corresponding [VersionParserTest](adoptopenjdk-api-v3-models/src/test/kotlin/net/adoptopenjdk/api/VersionParserTest.kt).

### Persistence

The layer that interacts with the Mongo DB cache.

### Updater

The layer that interacts with the AdoptOpenJDK JDK/JRE binary repositories on GitHub.

### Frontend

Contains the important [Platforms JSON](adoptopenjdk-api-v3-frontend/src/main/resources/JSON/platforms.json) and 
[Variants JSON](adoptopenjdk-api-v3-frontend/src/main/resources/JSON/variants.json).

## Common Tasks

In this section we list some common tasks and where to start.

### I want support a new version string

If you need to add/edit/remove a supported version string then you need to update the [VersionParser](adoptopenjdk-api-v3-models/src/main/kotlin/net/adoptopenjdk/api/v3/parser/VersionParser.kt) and 
its corresponding [VersionParserTest](adoptopenjdk-api-v3-models/src/test/kotlin/net/adoptopenjdk/api/VersionParserTest.kt).

### I want to add a new variant such as OpenJDK's project amber or 

You'll need to start at the [Platforms JSON](adoptopenjdk-api-v3-frontend/src/main/resources/JSON/platforms.json) and 
[Variants JSON](adoptopenjdk-api-v3-frontend/src/main/resources/JSON/variants.json).