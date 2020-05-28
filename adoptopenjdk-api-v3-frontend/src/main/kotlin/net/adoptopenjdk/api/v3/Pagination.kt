package net.adoptopenjdk.api.v3

import javax.ws.rs.NotFoundException
import kotlin.math.min

object Pagination {
    private const val defaultPageSizeNum = 10
    private const val maxPageSizeNum = 20
    const val defaultPageSize = defaultPageSizeNum.toString()
    const val maxPageSize = maxPageSizeNum.toString()

    fun <T> getPage(pageSize: Int?, page: Int?, releases: Sequence<T>): List<T> {
        val pageSizeNum = min(maxPageSizeNum, (pageSize ?: defaultPageSizeNum))
        val pageNum = page ?: 0

        val chunked = releases.chunked(pageSizeNum)

        return try {
            chunked.elementAt(pageNum)
        } catch (e: IndexOutOfBoundsException) {
            throw NotFoundException("Page not available")
        }
    }
}
