#!/bin/bash

# Temporary hack until patch is release on main quarkus project

git clone https://github.com/quarkusio/quarkus.git
cd quarkus
git fetch origin 2.1.1.Final
git checkout 2.1.1.Final
git apply ../release.patch
cd independent-projects/resteasy-reactive/server/vertx
../../../../mvnw clean install
