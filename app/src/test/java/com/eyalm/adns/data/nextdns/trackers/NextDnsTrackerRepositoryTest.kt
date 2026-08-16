package com.eyalm.adns.data.nextdns.trackers

import com.eyalm.adns.data.nextdns.api.NextDnsApi
import com.eyalm.adns.domain.nextdns.ApiResult
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class NextDnsTrackerRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var api: NextDnsApi
    private lateinit var repository: NextDnsTrackerRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(NextDnsApi::class.java)
        repository = NextDnsTrackerRepository(api)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `getTrackerInfo parses response correctly and caches repeated lookups`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "id": "google",
                      "name": "Google",
                      "category": "advertising",
                      "website": "https://www.google.com",
                      "company": {
                        "name": "Google",
                        "description": "Google LLC is an American multinational technology company...",
                        "privacyUrl": "http://www.google.com/intl/en/policies/privacy"
                      },
                      "prevalence": 0.2123
                    }
                    """.trimIndent()
                )
        )

        val result = repository.getTrackerInfo("google")

        assertTrue(result is ApiResult.Success)
        val tracker = (result as ApiResult.Success).value
        assertEquals("google", tracker.id)
        assertEquals("Google", tracker.name)
        assertEquals("advertising", tracker.category)
        assertEquals("https://www.google.com", tracker.website)
        assertEquals(0.2123f, tracker.prevalence ?: 0f, 0.0001f)
        assertEquals("Google", tracker.company?.name)
        assertEquals("http://www.google.com/intl/en/policies/privacy", tracker.company?.privacyUrl)

        // Second call should return cached result without making another network request
        val cachedResult = repository.getTrackerInfo("google")
        assertTrue(cachedResult is ApiResult.Success)
        assertEquals("google", (cachedResult as ApiResult.Success).value.id)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `getTrackerInfo with blank id returns failure without network call`() = runTest {
        val result = repository.getTrackerInfo("   ")
        assertTrue(result is ApiResult.SerializationFailure)
        assertEquals(0, server.requestCount)
    }
}
