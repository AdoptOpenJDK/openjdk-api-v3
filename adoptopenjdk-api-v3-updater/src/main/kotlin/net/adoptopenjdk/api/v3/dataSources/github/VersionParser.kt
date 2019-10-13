package net.adoptopenjdk.api.v3.dataSources.github

import net.adoptopenjdk.api.v3.models.VersionData
import org.slf4j.LoggerFactory
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

//This is a port of the Groovy VersionParser in the openjdk-build project
//Should probably look at exporting it as a common lib rather than having 2 implementations
class VersionParser {

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(this::class.java)
    }

    private var major: Int? = null
    private var minor: Int? = null
    private var security: Int? = null
    private var build: Int? = null
    private var opt: String? = null
    private var version: String? = null
    private var pre: String? = null
    private var adopt_build_number: Int? = null

    fun parse(publishName: String?, adoptBuildNumber: String? = null): VersionData {
        if (publishName != null) {
            if (!matchAdoptSemver(publishName)) {
                if (!matchPre223(publishName)) {
                    match223(publishName)
                }
            }
        }

        if (adoptBuildNumber != null && adopt_build_number == null) {
            adopt_build_number = Integer.parseInt(adoptBuildNumber)
        }
        return VersionData(major!!, minor!!, security!!, pre, adopt_build_number, build!!, opt, version!!)
    }

    private fun getOrDefaultNumber(matched: Matcher, groupName: String, defautNum: Int = 0): Int {
        if (!matched.pattern().pattern().contains(groupName)) {
            return defautNum
        }
        val number = matched.group(groupName)
        return if (number != null) {
            try {
                Integer.parseInt(number)
            } catch (e: NumberFormatException) {
                LOGGER.warn("failed to match ${number}")
                throw e
            }
        } else {
            defautNum
        }

    }

    private fun matchAltPre223(versionString: String): Boolean {
        //1.8.0_202-internal-201903130451-b08
        val pre223regex = """(?<version>1\.(?<major>[0-8])\.0(_(?<update>[0-9]+))?(-?(?<additional>.*))?)"""
        val matched = Pattern.compile(".*?$pre223regex.*?").matcher(versionString)

        if (matched.matches()) {
            major = getOrDefaultNumber(matched, "major")
            minor = 0
            security = getOrDefaultNumber(matched, "update")
            if (matched.group("additional") != null) {
                val additional = matched.group("additional")

                for (value in additional.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {

                    var matcher = Pattern.compile("""b(?<build>[0-9]+)""").matcher(value)
                    if (matcher.matches()) build = Integer.parseInt(matcher.group("build"))

                    matcher = Pattern.compile("""^(?<opt>[0-9]{12})$""").matcher(value)
                    if (matcher.matches()) opt = matcher.group("opt")
                }
            }

            version = matched.group("version")
            return true
        }

        return false
    }

    private fun matchPre223(versionString: String): Boolean {

        val majorMatcher = "(?<major>[0-8]+)"
        val update = "u(?<update>[0-9]+)"
        val buildMatcher = "-?b(?<build>[0-9]+)"
        val optMatcher = "_(?<opt>[-a-zA-Z0-9\\.]+)"
        val versionMatcher = "(?<version>$majorMatcher($update)($buildMatcher)?($optMatcher)?)"
        val prefixMatcher = "(jdk\\-?)"

        val pre223regex = ".*?$prefixMatcher?$versionMatcher.*?"

        val matched = Pattern.compile(pre223regex).matcher(versionString)

        if (matched.matches()) {
            major = matched.group("major").toInt()
            minor = 0
            security = getOrDefaultNumber(matched, "update")
            build = getOrDefaultNumber(matched, "build")
            if (matched.group("opt") != null) opt = matched.group("opt")
            version = matched.group("version")
            return true
        } else {
            return matchAltPre223(versionString)
        }

    }


    private fun matchAdoptSemver(versionString: String): Boolean {
        //Regexes based on those in http://openjdk.java.net/jeps/223
        // Technically the standard supports an arbitrary number of numbers, we will support 3 for now
        val vnumRegex = """(?<major>[0-9]+)\.(?<minor>[0-9]+)\.(?<security>[0-9]+)"""
        val preRegex = "(?<pre>[a-zA-Z0-9]+)"
        val buildRegex = "(?<build>[0-9]+)\\.(?<adoptBuild>[0-9]+)"
        val optRegex = "(?<opt>[-a-zA-Z0-9\\.]+)"

        val regex = "(?:jdk\\-)?(?<version>$vnumRegex(\\-$preRegex)?\\+$buildRegex(\\-$optRegex)?)"

        val matched = Pattern.compile(".*?$regex.*?").matcher(versionString)
        if (matched.matches()) {
            major = matched.group("major").toInt()
            minor = getOrDefaultNumber(matched, "minor")
            security = getOrDefaultNumber(matched, "security")
            if (regex.contains("pre") && matched.group("pre") != null) {
                pre = matched.group("pre")
            }
            build = getOrDefaultNumber(matched, "build")
            adopt_build_number = getOrDefaultNumber(matched, "adoptBuild", 1)
            if (matched.group("opt") != null) {
                opt = matched.group("opt")
            }
            version = matched.group("version")
            return true
        }

        return false
    }

    private fun match223(versionString: String): Boolean {
        //Regexes based on those in http://openjdk.java.net/jeps/223
        // Technically the standard supports an arbitrary number of numbers, we will support 3 for now
        val vnumRegex = """(?<major>[0-9]+)(\.(?<minor>[0-9]+))?(\.(?<security>[0-9]+))?"""
        val preRegex = "(?<pre>[a-zA-Z0-9]+)"
        val buildRegex = "(?<build>[0-9]+)"
        val optRegex = "(?<opt>[-a-zA-Z0-9\\.]+)"

        val version223Regexs = Arrays.asList(
                "(?:jdk\\-)?(?<version>$vnumRegex(\\-$preRegex)?\\+$buildRegex(\\-$optRegex)?)",
                "(?:jdk\\-)?(?<version>$vnumRegex\\-$preRegex(\\-$optRegex)?)",
                "(?:jdk\\-)?(?<version>$vnumRegex(\\+\\-$optRegex)?)")

        for (regex in version223Regexs) {
            val matched223 = Pattern.compile(".*?$regex.*?").matcher(versionString)
            if (matched223.matches()) {
                major = matched223.group("major").toInt()
                minor = getOrDefaultNumber(matched223, "minor")
                security = getOrDefaultNumber(matched223, "security")
                if (regex.contains("pre") && matched223.group("pre") != null) {
                    pre = matched223.group("pre")
                }
                build = getOrDefaultNumber(matched223, "build")
                if (matched223.group("opt") != null) {
                    opt = matched223.group("opt")
                }
                version = matched223.group("version")
                return true
            }
        }

        return false
    }


}
