# Contributing to AdoptOpenJDK API (V3)

## Overview

The AdoptOpenJDK API V3 is a Kotlin application (fronted by Swagger and OpenAPI) which makes 
calls via the GitHub API inb order to retrieve AdoptOpenJDK binaries and metadata.

Since the GitHub API is rate limited we use MongoDB as a caching mechanism.

## Build

### Pre-Requisites

Java 11 is required to build.

### Build Tool

We use a Maven Wrapper (mvnw) to ensure that there's a consistent repeatable build.

### Build

`./mvnw clean install`

## Usage

The api is documented via swagger.  The swagger documentation can be viewed at: [swagger-ui](https://api.adoptopenjdk.net/swagger-ui). 
The open api definition for this can be viewed at [openapi](https://api.adoptopenjdk.net/openapi).

## Deployment

The API is deployed to Red Hat OpenShift.

The API is CDN fronted by Cloud Flare
