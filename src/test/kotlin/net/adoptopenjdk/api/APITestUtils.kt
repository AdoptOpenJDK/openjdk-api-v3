package net.adoptopenjdk.api


import com.google.common.base.Joiner
import org.junit.jupiter.api.DynamicTest
import java.util.stream.Stream

open class APITestUtils {


    companion object {

        /**
         * Generates all permutations of a set of lists, i.e given:
         * [a,b], [c,d], [e,f]
         *
         * Generates:
         *
         * a,c,e
         * a,c,f
         * a,d,e
         * a,d,f
         * b,c,e
         * b,c,f
         * b,d,e
         * b,d,f
         *
         */
        fun createPermutations(arg: List<List<Any>>, permutationLimit: Int = Int.MAX_VALUE): List<List<Any>> {
            if (arg.size == 1) {
                return arg.get(0).map { listOf(it) }
            }

            val head: List<Any> = arg.get(0)
            val tail: List<List<Any>> = arg.drop(1)
            val x = createPermutations(tail)
                    .flatMap { tailList ->
                        head.map {
                            listOf(it) + tailList
                        }
                    }

            val chunkSize = Math.max(1, x.size / permutationLimit)
            if (chunkSize == 1) return x

            return x.chunked(chunkSize, { it.first() })
        }


        /**
         * Run a test against all permutations provided
         */
        fun runTest(
                perms: List<List<Any>>,
                test: ((a: List<Any>) -> Unit)): Stream<DynamicTest> {
            return perms.map { args ->
                val name = Joiner.on(",").join(args)
                DynamicTest.dynamicTest(name, { test(args) })

            }.stream()
        }
/*
        fun releaseVersionPermutations(
                test: (
                        releaseType: ReleaseType,
                        releaseVersion: Int
                ) -> Unit): Stream<DynamicTest> {
            return runTest(createPermutations(listOf(
                    names(ReleaseType.values()),
                    names(ReleaseVersion.values()))
            ), { args ->
                val releaseType: ReleaseType = ReleaseType.valueOf(args.get(0).toString())
                val releaseVersion = args.get(1)
                test(releaseType, releaseVersion.toString().toInt())
            })
        }*/

        fun <E : Enum<*>> names(values: Array<E>): List<String> {
            return values.map { it.name }
        }
    }
}

