package net.adoptopenjdk.api.v3.dataSources.filters

import net.adoptopenjdk.api.v3.models.Architecture
import net.adoptopenjdk.api.v3.models.Binary
import net.adoptopenjdk.api.v3.models.HeapSize
import net.adoptopenjdk.api.v3.models.ImageType
import net.adoptopenjdk.api.v3.models.JvmImpl
import net.adoptopenjdk.api.v3.models.OperatingSystem
import net.adoptopenjdk.api.v3.models.Project
import java.util.function.Predicate

class BinaryFilter : Predicate<Binary> {

    private val os: OperatingSystem?
    private val arch: Architecture?
    private val imageType: ImageType?
    private val jvmImpl: JvmImpl?
    private val heapSize: HeapSize?
    private val project: Project

    constructor(
            os: OperatingSystem?,
            arch: Architecture?,
            imageType: ImageType?,
            jvmImpl: JvmImpl?,
            heapSize: HeapSize?,
            project: Project?
    ) {
        this.os = os
        this.arch = arch
        this.imageType = imageType
        this.jvmImpl = jvmImpl
        this.heapSize = heapSize
        this.project = project ?: Project.jdk
    }

    override fun test(binary: Binary): Boolean {
        return (os == null || binary.os == os) &&
                (arch == null || binary.architecture == arch) &&
                (imageType == null || binary.image_type == imageType) &&
                (jvmImpl == null || binary.jvm_impl == jvmImpl) &&
                (heapSize == null || binary.heap_size == heapSize) &&
                (binary.project == project)
    }

}
