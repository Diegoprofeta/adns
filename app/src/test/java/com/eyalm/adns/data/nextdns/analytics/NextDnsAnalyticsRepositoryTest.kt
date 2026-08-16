package com.eyalm.adns.data.nextdns.analytics

import com.eyalm.adns.data.nextdns.api.NextDnsApi
import com.eyalm.adns.domain.nextdns.ApiResult
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class NextDnsAnalyticsRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: NextDnsAnalyticsRepository

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NextDnsApi::class.java)
        repository = NextDnsAnalyticsRepository(api)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `device and selected period are sent to graph and card endpoints`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"data":[],"meta":{}}""")
        )
        val scope = AnalyticsScope(AnalyticsPeriod.Hours6, "device-id")

        val graph = repository.getGraph("profile-id", scope)

        assertTrue(graph is ApiResult.Success)
        val graphRequest = server.takeRequest().requestUrl!!
        assertEquals("-6h", graphRequest.queryParameter("from"))
        assertEquals("device-id", graphRequest.queryParameter("device"))

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"data":[]}""")
        )
        val card = repository.getCardData(
            profileId = "profile-id",
            feature = "domains",
            baseParams = mapOf("limit" to "6"),
            scope = scope,
        )

        assertTrue(card is ApiResult.Success)
        val cardRequest = server.takeRequest().requestUrl!!
        assertEquals("-6h", cardRequest.queryParameter("from"))
        assertEquals("device-id", cardRequest.queryParameter("device"))
        assertEquals("6", cardRequest.queryParameter("limit"))
    }

    @Test
    fun `getCardSeries for domains uses status series and returns blocked points`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "data": [
                        { "status": "default", "queries": [100, 200, 300] },
                        { "status": "blocked", "queries": [15, 35, 55] }
                      ],
                      "meta": {
                        "series": {
                          "times": ["2026-08-01", "2026-08-02", "2026-08-03"],
                          "interval": 86400
                        }
                      }
                    }
                    """.trimIndent()
                )
        )
        val card = ListCard(
            key = "domains.blocked",
            feature = "domains",
            params = mapOf("status" to "blocked", "limit" to "6"),
            localePath = emptyList(),
            emptyPath = emptyList(),
            kind = ListKind.DOMAINS
        )
        val scope = AnalyticsScope(AnalyticsPeriod.Days7, "device-1")
        val result = repository.getCardSeries(
            profileId = "profile-123",
            card = card,
            scope = scope,
        )

        assertTrue(result is ApiResult.Success)
        val series = (result as ApiResult.Success).value
        assertEquals(listOf(15f, 35f, 55f), series.points)
        assertEquals(105, series.totalQueries)
        assertEquals(1, series.seriesList.size)
        assertEquals("Blocked", series.seriesList[0].name)
        assertEquals(105, series.seriesList[0].totalQueries)

        val request = server.takeRequest().requestUrl!!
        assertTrue(request.pathSegments.contains("status;series"))
        assertEquals("-7d", request.queryParameter("from"))
        assertEquals("device-1", request.queryParameter("device"))
        assertEquals("start", request.queryParameter("alignment"))
    }

    @Test
    fun `getCardSeries for reasons calls reasons series endpoint and aggregates points`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "data": [
                        { "id": "blocklist-1", "name": "HaGeZi", "queries": [10, 20, 30] },
                        { "id": "blocklist-2", "name": "NextDNS Recommended", "queries": [5, 15, 25] }
                      ],
                      "meta": {
                        "series": {
                          "times": ["2026-08-01", "2026-08-02", "2026-08-03"],
                          "interval": 86400
                        }
                      }
                    }
                    """.trimIndent()
                )
        )
        val card = ListCard(
            key = "reasons",
            feature = "reasons",
            params = mapOf("limit" to "6", "lang" to "en"),
            localePath = emptyList(),
            emptyPath = emptyList(),
            kind = ListKind.REASONS
        )
        val scope = AnalyticsScope(AnalyticsPeriod.Days7)
        val result = repository.getCardSeries(
            profileId = "profile-123",
            card = card,
            scope = scope,
        )

        assertTrue(result is ApiResult.Success)
        val series = (result as ApiResult.Success).value
        assertEquals(listOf(15f, 35f, 55f), series.points)
        assertEquals(105, series.totalQueries)
        assertEquals(2, series.seriesList.size)
        assertEquals("HaGeZi", series.seriesList[0].name)
        assertEquals(60, series.seriesList[0].totalQueries)
        assertEquals("NextDNS Recommended", series.seriesList[1].name)
        assertEquals(45, series.seriesList[1].totalQueries)

        val request = server.takeRequest().requestUrl!!
        assertTrue(request.pathSegments.contains("reasons;series"))
        assertEquals("-7d", request.queryParameter("from"))
        assertEquals("start", request.queryParameter("alignment"))
    }

    @Test
    fun `getExpandedCardData passes limit cursor parameters and parses next cursor`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "data": [
                        { "domain": "ads.example.com", "queries": 120 },
                        { "domain": "analytics.example.com", "queries": 80 }
                      ],
                      "meta": {
                        "pagination": {
                          "cursor": "next-cursor-xyz"
                        }
                      }
                    }
                    """.trimIndent()
                )
        )
        val card = ListCard(
            key = "domains.blocked",
            feature = "domains",
            params = mapOf("status" to "blocked"),
            localePath = emptyList(),
            emptyPath = emptyList(),
            kind = ListKind.DOMAINS
        )
        val scope = AnalyticsScope(AnalyticsPeriod.Days30)

        val result = repository.getExpandedCardData(
            profileId = "prof-1",
            card = card,
            scope = scope,
            cursor = "current-cursor-abc",
            limit = 100,
        )

        assertTrue(result is ApiResult.Success)
        val page = (result as ApiResult.Success).value
        assertEquals(2, page.rows.size)
        assertEquals("ads.example.com", page.rows[0].title)
        assertEquals("120", page.rows[0].value)
        assertEquals("next-cursor-xyz", page.cursor)

        val request = server.takeRequest().requestUrl!!
        assertEquals("-30d", request.queryParameter("from"))
        assertEquals("100", request.queryParameter("limit"))
        assertEquals("current-cursor-abc", request.queryParameter("cursor"))
        assertEquals("blocked", request.queryParameter("status"))
        assertEquals(null, request.queryParameter("search"))
    }
}

