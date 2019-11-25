package net.adoptopenjdk.api.v3.dataSources.filters

import net.adoptopenjdk.api.v3.models.*
import java.util.function.Predicate

class BinaryFilter(
        private val os: OperatingSystem?,
        private val arch: Architecture?,
        private val imageType: ImageType?,
        private val jvmImpl: JvmImpl?,
        private val heapSize: HeapSize?,
        private val project: Project?
) : Predicate<Binary> {

    override fun test(binary: Binary): Boolean {
        return (os == null || binary.os == os) &&
                (arch == null || binary.architecture == arch) &&
                (imageType == null || binary.image_type == imageType) &&
                (jvmImpl == null || binary.jvm_impl == jvmImpl) &&
                (heapSize == null || binary.heap_size == heapSize) &&
                (project == null || binary.project == project)
    }

}
