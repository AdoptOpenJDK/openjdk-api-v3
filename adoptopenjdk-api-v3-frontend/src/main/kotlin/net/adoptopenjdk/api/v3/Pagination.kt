package net.adoptopenjdk.api.v3

import javax.ws.rs.NotFoundException
import kotlin.math.min

object Pagination {
    private const val defaultPageSizeNum = 10
    private const val MAX_PAGE_SIZE = 20
    const val defaultPageSize = defaultPageSizeNum.toString()
    const val maxPageSize = MAX_PAGE_SIZE.toString()

    fun <T> getPage(pageSize: Int?, page: Int?, releases: Sequence<T>, maxPageSize: Int = MAX_PAGE_SIZE): List<T> {
        val pageSizeNum = min(maxPageSize, (pageSize ?: defaultPageSizeNum))
        val pageNum = page ?: 0

        val chunked = releases.chunked(pageSizeNum)

        return try {
            chunked.elementAt(pageNum)
        } catch (e: IndexOutOfBoundsException) {
            throw NotFoundException("Page not available")
        }
    }
}
