package net.adoptopenjdk.api.v3.mapping.adopt

import net.adoptopenjdk.api.v3.models.VersionData
import java.util.regex.Pattern

data class SemVer(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val pre: String?,
    val preSegments: List<String>,
    val buildMetadata: String?,
    val buildMetadataSegments: List<String>
)

class SemVerParser {
    companion object {
        //Regex provided by https://semver.org/
        val REGEX = Pattern.compile("""^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-((?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\+([0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?$""")!!

        fun parse(version: String): SemVer {
            val matcher = REGEX.matcher(version)

            var pre: String? = null
            var preSegments = listOf<String>()

            if (matcher.matches()) {
                if (matcher.group(4) != null) {
                    pre = matcher.group(4)
                    preSegments = pre.split(".")
                }

                var build: String? = null
                var buildSegments = listOf<String>()
                if (matcher.group(5) != null) {
                    build = matcher.group(5)
                    buildSegments = build.split(".")
                }

                return SemVer(
                    matcher.group(1).toInt(),
                    matcher.group(2).toInt(),
                    matcher.group(3).toInt(),
                    pre,
                    preSegments,
                    build,
                    buildSegments
                )
            } else {
                throw RuntimeException("foo")
            }
        }
    }
}
