package net.adoptopenjdk.api.v3.parser

import net.adoptopenjdk.api.v3.models.VersionData
import org.slf4j.LoggerFactory
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

//This is a port of the Groovy VersionParser in the openjdk-build project
//Should probably look at exporting it as a common lib rather than having 2 implementations
object VersionParser {

    @JvmStatic
    private val LOGGER = LoggerFactory.getLogger(this::class.java)
    private val REGEXES: List<Pattern> = getRegexes()
    private val DATE_TIME_MATCHER: Regex = Regex("^[0-9]{4}(-[0-9]{1,2}){4}$")
    private val PRE_223_REGEX: Pattern = Pattern.compile(""".*?(?<version>1\.(?<major>[0-8])\.0(_(?<update>[0-9]+))?(-?(?<additional>.*))?).*?""")


    private fun pre223(): String {
        val majorMatcher = "(?<major>[0-8]+)"
        val security = "u(?<security>[0-9]+)"
        val buildMatcher = "-?b(?<build>[0-9]+)"
        val optMatcher = "_(?<opt>[-a-zA-Z0-9\\.]+)"
        val versionMatcher = "(?<version>$majorMatcher($security)($buildMatcher)?($optMatcher)?)"
        val prefixMatcher = "(jdk\\-?)"

        return "$prefixMatcher?$versionMatcher"
    }

    private fun adoptSemver(): String {
        val vnumRegex = """(?<major>[0-9]+)\.(?<minor>[0-9]+)\.(?<security>[0-9]+)"""
        val preRegex = "(?<pre>[a-zA-Z0-9]+)"
        val buildRegex = "(?<build>[0-9]+)(\\.(?<adoptBuild>[0-9]+))?"
        val optRegex = "(?<opt>[-a-zA-Z0-9\\.]+)"

        return "(?:jdk\\-)?(?<version>$vnumRegex(\\-$preRegex)?\\+$buildRegex(\\-$optRegex)?)"
    }

    //Identical to java version but with adoptbuild number
    // i.e allow . in the build number
    private fun jep223WithAdoptBuildNum(): List<String> {
        //Regexes based on those in http://openjdk.java.net/jeps/223
        // Technically the standard supports an arbitrary number of numbers, we will support 3 for now
        val vnumRegex = """(?<major>[0-9]+)(\.(?<minor>[0-9]+))?(\.(?<security>[0-9]+))?"""
        val preRegex = "(?<pre>[a-zA-Z0-9]+)"
        val buildRegex = "(?<build>[0-9]+)(\\.(?<adoptBuild>[0-9]+))?"
        val optRegex = "(?<opt>[-a-zA-Z0-9\\.]+)"

        return Arrays.asList(
                "(?:jdk\\-)?(?<version>$vnumRegex(\\-$preRegex)?\\+$buildRegex(\\-$optRegex)?)",
                "(?:jdk\\-)?(?<version>$vnumRegex\\-$preRegex(\\-$optRegex)?)",
                "(?:jdk\\-)?(?<version>$vnumRegex(\\+\\-$optRegex)?)")
    }

    private fun jep223(): List<String> {
        //Regexes based on those in http://openjdk.java.net/jeps/223
        // Technically the standard supports an arbitrary number of numbers, we will support 3 for now
        val vnumRegex = """(?<major>[0-9]+)(\.(?<minor>[0-9]+))?(\.(?<security>[0-9]+))?"""
        val preRegex = "(?<pre>[a-zA-Z0-9]+)"
        val buildRegex = "(?<build>[0-9]+)"
        val optRegex = "(?<opt>[-a-zA-Z0-9\\.]+)"

        return Arrays.asList(
                "(?:jdk\\-)?(?<version>$vnumRegex(\\-$preRegex)?\\+$buildRegex(\\-$optRegex)?)",
                "(?:jdk\\-)?(?<version>$vnumRegex\\-$preRegex(\\-$optRegex)?)",
                "(?:jdk\\-)?(?<version>$vnumRegex(\\+\\-$optRegex)?)")
    }


    private fun adoptNightly(): String {
        return """jdk(?<version>(?<major>[0-9]+)[-u]+(?<opt>[-0-9]+))"""
    }

    private fun getRegexes(): List<Pattern> {
        val regexes = mutableListOf(adoptSemver())
        regexes.addAll(jep223WithAdoptBuildNum())
        regexes.addAll(jep223())
        regexes.add(pre223())
        regexes.add(adoptNightly())

        return regexes.map { regex -> Pattern.compile(".*?$regex.*?") }
    }

    fun parse(publishName: String?): VersionData {
        if (publishName == null) {
            throw FailedToParse("null name")
        }
        try {
            var version = matchVersion(publishName)
            if (version != null) {
                return version
            }

            version = parseWithJavaClass(publishName)
            if (version != null) {
                return version
            }

            version = matchAltPre223(publishName)
            if (version != null) {
                return version
            }
        } catch (e: Exception) {
        }
        throw FailedToParse("Failed to parse ${publishName}")
    }

    private fun parseWithJavaClass(publishName: String): VersionData? {

        try {
            val parsedVersion = Runtime.Version.parse(publishName.removePrefix("jdk"))
            val major = parsedVersion.feature()
            val minor = parsedVersion.interim()
            val security = parsedVersion.update()
            val build = parsedVersion.build().orElse(null)
            val opt = parsedVersion.optional().orElse(null)
            val version = parsedVersion.toString()
            val pre = parsedVersion.pre().orElse(null)

            val parsed = VersionData(major, minor, security, pre, 1, build!!, opt, version)
            if (sanityCheck(parsed)) {
                return parsed
            }
        } catch (e: Exception) {
        }
        return null
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
                LOGGER.warn("failed to match $number")
                throw e
            }
        } else {
            defautNum
        }

    }

    private fun matchAltPre223(versionString: String): VersionData? {
        //1.8.0_202-internal-201903130451-b08
        val matched = PRE_223_REGEX.matcher(versionString)

        if (matched.matches()) {
            val major = getOrDefaultNumber(matched, "major")
            val minor = 0
            val security = getOrDefaultNumber(matched, "update")
            var build = 0
            var opt: String? = null
            val additional: String?
            if (matched.group("additional") != null) {
                additional = matched.group("additional")

                for (value in additional.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {

                    var matcher = Pattern.compile("""b(?<build>[0-9]+)""").matcher(value)
                    if (matcher.matches()) build = Integer.parseInt(matcher.group("build"))

                    matcher = Pattern.compile("""^(?<opt>[0-9]{12})$""").matcher(value)
                    if (matcher.matches()) opt = matcher.group("opt")
                }
            }

            val version = matched.group("version")

            val parsed = VersionData(major, minor, security, null, 1, build, opt, version)
            if (sanityCheck(parsed)) {
                return parsed
            }
        }

        return null
    }

    private fun sanityCheck(parsed: VersionData): Boolean {

        if (!(parsed.major in 101 downTo 7)) {
            //Sanity check as javas parser can match a single number
            // sane range is 8 to 100
            // TODO update me before 2062 and java 100 is released
            return false
        }
        if (parsed.security != 0 || parsed.build != 0) {
            return true
        }
        if (parsed.optional != null && parsed.optional.matches(DATE_TIME_MATCHER)) {
            return true
        }
        return false
    }

    private fun matchVersion(versionString: String): VersionData? {
        REGEXES
                .forEach { regex ->
                    val matched = regex.matcher(versionString)
                    if (matched.matches()) {
                        val major = matched.group("major").toInt()
                        val minor = getOrDefaultNumber(matched, "minor")
                        val security = getOrDefaultNumber(matched, "security")
                        var pre: String? = null
                        if (regex.pattern().contains("pre") && matched.group("pre") != null) {
                            pre = matched.group("pre")
                        }
                        val build = getOrDefaultNumber(matched, "build")
                        val adopt_build_number = getOrDefaultNumber(matched, "adoptBuild")
                        var opt: String? = null
                        if (matched.group("opt") != null) {
                            opt = matched.group("opt")
                        }
                        val version = matched.group("version")

                        val parsed = VersionData(major, minor, security, pre, adopt_build_number, build, opt, version)
                        if (sanityCheck(parsed)) {
                            return parsed
                        }
                    }
                }
        return null
    }
}

class FailedToParse(message: String) : Exception(message)
