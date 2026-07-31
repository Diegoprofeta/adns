package com.eyalm.adns.data.nextdns.connection

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class NextDnsConnectionRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var api: NextDnsConnectionApi

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NextDnsConnectionApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `unconfigured probe resolves the detected resolver name`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"status":"unconfigured","resolver":"74.63.24.205","srcIP":"77.137.71.49","server":"anexia-fra-1"}"""
            )
        )
        server.enqueue(MockResponse().setBody("""{"name":"Quad9"}"""))
        val repository = NextDnsConnectionRepository(api) { server.url("probe").toString() }

        val status = repository.detect("29a59d", "fp-selected")

        assertTrue(status is NextDnsConnectionStatus.NotUsingNextDns)
        val notUsing = status as NextDnsConnectionStatus.NotUsingNextDns
        assertEquals("Quad9", notUsing.resolverName)
        assertEquals("74.63.24.205", notUsing.probe.resolver)
        assertEquals("/probe", server.takeRequest().path)
        assertEquals("/resolver/74.63.24.205", server.takeRequest().path)
    }

    @Test
    fun `matching profile probe does not perform resolver lookup`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"status":"ok","profile":"fp-selected","protocol":"DOH","deviceName":"phone"}"""
            )
        )
        val repository = NextDnsConnectionRepository(api) { server.url("probe").toString() }

        val status = repository.detect("29a59d", "fp-selected")

        assertTrue(status is NextDnsConnectionStatus.UsingSelectedProfile)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `mismatch without profile is classified separately`() {
        val status = classifyConnectionProbe(
            NextDnsConnectionProbe(status = "mismatch", profile = null),
            selectedFingerprint = "fp-selected",
        )

        assertTrue(status is NextDnsConnectionStatus.UsingNextDnsWithoutProfile)
    }

    @Test
    fun `different returned fingerprint reports different profile`() {
        val status = classifyConnectionProbe(
            NextDnsConnectionProbe(status = "ok", profile = "fp-other"),
            selectedFingerprint = "fp-selected",
        )

        assertTrue(status is NextDnsConnectionStatus.UsingDifferentProfile)
        val different = status as NextDnsConnectionStatus.UsingDifferentProfile
        assertEquals("fp-other", different.profileFingerprint)
    }

    @Test
    fun `probe hostname uses randomized lowercase label and profile id`() {
        val first = nextDnsProbeUrl("29a59d")
        val second = nextDnsProbeUrl("29a59d")
        val pattern = Regex("https://[a-z0-9]{12}-29a59d\\.test\\.nextdns\\.io/")

        assertEquals(true, pattern.matches(first))
        assertEquals(true, pattern.matches(second))
        assertFalse(first == second)
        assertEquals("", nextDnsProbeUrl(""))
    }
}
