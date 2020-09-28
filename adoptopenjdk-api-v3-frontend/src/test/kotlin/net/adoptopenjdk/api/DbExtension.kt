package net.adoptopenjdk.api

import net.adoptopenjdk.api.v3.V3.Companion.ENABLE_PERIODIC_UPDATES
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext

class DbExtension : BeforeAllCallback {
    override fun beforeAll(context: ExtensionContext?) {
        System.setProperty(ENABLE_PERIODIC_UPDATES, "false")
    }
}
