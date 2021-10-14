**NOTE:** This repo is now deprecated/archived as github.com/adoptium/api.adoptium.net runs both api.adoptium.net and api.adoptopenjdk.net using vendor properties as a filter to do the correct thing on each domain.


![Build](https://github.com/AdoptOpenJDK/openjdk-api-v3/workflows/Build/badge.svg?branch=master) [![codecov](https://codecov.io/gh/AdoptOpenJDK/openjdk-api-v3/branch/master/graph/badge.svg)](https://codecov.io/gh/AdoptOpenJDK/openjdk-api-v3)

# AdoptOpenJDK API

**NOTICE:** AdoptOpenJDK API v1 has now been removed.
If you are using v1 please move to the latest version (documented below) as soon as possible.

**NOTICE:** [AdoptOpenJDK API v2](https://github.com/AdoptOpenJDK/openjdk-api/blob/master/README.md) has been deprecated and will be removed.
If you are using v2 please move to the latest version (documented below) as soon as possible.

## Overview

The AdoptOpenJDK API provides a way to consume JSON information about the AdoptOpenJDK releases and nightly builds.  
Sign up to the [mailing list](https://mail.openjdk.java.net/mailman/listinfo/adoption-discuss) where major API updates will be announced, and visit [adoptopenjdk.net](https://adoptopenjdk.net) to find out more about the community.

To learn more about how we build & run the API, check out [CONTRIBUTING.md](CONTRIBUTING.md) and the [FAQs](FAQ.md).

## Usage

The API is documented via swagger.  The swagger documentation can be viewed at: [swagger-ui](https://api.adoptopenjdk.net/swagger-ui). 
The open api definition for this can be viewed at [openapi](https://api.adoptopenjdk.net/openapi).

For more information, including example queries, please look at [STRUCTURE.md](https://github.com/AdoptOpenJDK/openjdk-api-v3/blob/master/docs/STRUCTURE.md)

## Who's using the AdoptOpenJDK API?

The AdoptOpenJDK API has served over 200 million downloads by a wide variety consumers, from individuals to organisations.

Check the [Download Statistics Dashboard](https://dash.adoptopenjdk.net/) for the latest numbers.  

The following list highlights a small subset of consumers and their use-cases:

- [AdoptOpenJDK Website](https://adoptopenjdk.net/) - the API drives the release listings on the AdoptOpenJDK website allowing individuals to download the JDK distribution of their choice
- [AdoptOpenJDK Docker Images](https://github.com/AdoptOpenJDK/openjdk-docker) - the API is used during the creation of the various official & unofficial Docker images
- [Gradle](https://docs.gradle.org/) - the Gradle project defaults to use the API for its [toolchains](https://docs.gradle.org/current/userguide/toolchains.html#sec:provisioning) feature
- [Update Watcher for AdoptOpenJDK](https://github.com/tushev/aojdk-updatewatcher) - uses the API to automatically manage the JDK installations on an individual's machine   
