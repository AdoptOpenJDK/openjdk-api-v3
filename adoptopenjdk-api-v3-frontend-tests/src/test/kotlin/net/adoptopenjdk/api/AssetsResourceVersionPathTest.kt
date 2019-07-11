package net.adoptopenjdk.api

import io.quarkus.test.junit.QuarkusTest


@QuarkusTest
class AssetsResourceVersionPathTest : AssetsPathTest() {


    override fun getPath() = "/v3/assets/version"


}

