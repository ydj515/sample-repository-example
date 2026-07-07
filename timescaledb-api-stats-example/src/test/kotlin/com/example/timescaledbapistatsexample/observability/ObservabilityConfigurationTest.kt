package com.example.timescaledbapistatsexample.observability

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.yaml.snakeyaml.Yaml

class ObservabilityConfigurationTest {
    private val projectRoot: Path = Path.of("").toAbsolutePath()
    private val yaml = Yaml()
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `compose defines grafana prometheus and postgres exporter services`() {
        val compose = readYaml(projectRoot.resolve("docker-compose.yml"))
        val services = compose.map("services")

        val timescaledb = services.map("timescaledb")
        assertEquals("timescale/timescaledb:2.28.2-pg17", timescaledb["image"])

        val grafana = services.map("grafana")
        assertEquals("grafana/grafana:11.5.2", grafana["image"])
        assertEquals(listOf("3000:3000"), grafana["ports"])
        assertTrue(grafana.list("volumes").contains("./infra/grafana/provisioning:/etc/grafana/provisioning:ro"))
        assertTrue(grafana.list("volumes").contains("./infra/grafana/dashboards:/var/lib/grafana/dashboards:ro"))

        val prometheus = services.map("prometheus")
        assertEquals("prom/prometheus:v2.48.0", prometheus["image"])
        assertEquals(listOf("9090:9090"), prometheus["ports"])
        assertTrue(prometheus.list("volumes").contains("./infra/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro"))
        assertTrue(prometheus.list("extra_hosts").contains("host.docker.internal:host-gateway"))

        val postgresExporter = services.map("postgres-exporter")
        assertEquals("prometheuscommunity/postgres-exporter:v0.17.1", postgresExporter["image"])
        assertTrue(postgresExporter.list("environment").contains("DATA_SOURCE_URI=timescaledb:5432/api_stats?sslmode=disable"))
        assertTrue(postgresExporter.list("environment").contains("DATA_SOURCE_USER=api_stats"))
        assertTrue(postgresExporter.list("environment").contains("DATA_SOURCE_PASS=api_stats"))
    }

    @Test
    fun `prometheus scrapes spring boot and postgres exporter metrics separately`() {
        val prometheus = readYaml(projectRoot.resolve("infra/prometheus/prometheus.yml"))
        val scrapeConfigs = prometheus.listOfMaps("scrape_configs").associateBy { it["job_name"] as String }

        assertEquals(listOf(mapOf("targets" to listOf("host.docker.internal:8080"))), scrapeConfigs["spring-boot"]?.get("static_configs"))
        assertEquals(listOf(mapOf("targets" to listOf("postgres-exporter:9187"))), scrapeConfigs["postgres-exporter"]?.get("static_configs"))
        assertEquals(listOf(mapOf("targets" to listOf("prometheus:9090"))), scrapeConfigs["prometheus"]?.get("static_configs"))
    }

    @Test
    fun `grafana provisions prometheus and timescaledb datasources`() {
        val datasources = readYaml(projectRoot.resolve("infra/grafana/provisioning/datasources/datasources.yml"))
            .listOfMaps("datasources")
            .associateBy { it["uid"] as String }

        val prometheus = assertNotNull(datasources["prometheus"])
        assertEquals("Prometheus", prometheus["name"])
        assertEquals("prometheus", prometheus["type"])
        assertEquals("http://prometheus:9090", prometheus["url"])
        assertEquals(true, prometheus["isDefault"])

        val timescaledb = assertNotNull(datasources["timescaledb-api-stats"])
        assertEquals("TimescaleDB API Stats", timescaledb["name"])
        assertEquals("postgres", timescaledb["type"])
        assertEquals("timescaledb:5432", timescaledb["url"])
        assertEquals("api_stats", timescaledb["database"])
        assertEquals(true, timescaledb.map("jsonData")["timescaledb"])
    }

    @Test
    fun `grafana dashboard provisioning points to local dashboard files`() {
        val provider = readYaml(projectRoot.resolve("infra/grafana/provisioning/dashboards/dashboards.yml"))
        val providers = provider.listOfMaps("providers")

        assertEquals(1, providers.size)
        assertEquals("timescaledb-api-stats", providers.first()["name"])
        assertEquals("/var/lib/grafana/dashboards", providers.first().map("options")["path"])
        assertTrue(Files.exists(projectRoot.resolve("infra/grafana/dashboards/api-stats-timescaledb.json")))
        assertTrue(Files.exists(projectRoot.resolve("infra/grafana/dashboards/operations-prometheus.json")))
    }

    @Test
    fun `timescaledb dashboard includes api key period stats panels`() {
        val dashboardText = Files.readString(projectRoot.resolve("infra/grafana/dashboards/api-stats-timescaledb.json"))
        val dashboard = objectMapper.readTree(dashboardText)
        val variables = dashboard.path("templating").path("list").elements().asSequence()
            .associateBy { it.path("name").asText() }
        val panelTitles = dashboard.path("panels").elements().asSequence()
            .map { it.path("title").asText() }
            .toSet()

        assertTrue(!dashboardText.contains("status_code"), "Dashboard SQL should use the api_call_events.status column")
        assertEquals("custom", assertNotNull(variables["period"])["type"].asText())
        assertEquals("query", assertNotNull(variables["api_client"])["type"].asText())
        assertTrue(panelTitles.contains("API Key Calls by Period"))
        assertTrue(panelTitles.contains("API Key Route Details"))
    }

    @Test
    fun `schema defines api key continuous aggregates by day month and year`() {
        val schema = Files.readString(projectRoot.resolve("infra/timescaledb/init/01_schema.sql"))
        val viewNames = listOf(
            "api_key_call_stats_daily",
            "api_key_call_stats_monthly",
            "api_key_call_stats_yearly",
        )

        viewNames.forEach { viewName ->
            assertTrue(
                schema.contains("CREATE MATERIALIZED VIEW IF NOT EXISTS $viewName"),
                "Expected continuous aggregate view to exist: $viewName",
            )
            assertTrue(
                schema.contains("WITH (timescaledb.continuous)"),
                "Expected TimescaleDB continuous aggregate option",
            )
            assertTrue(
                schema.contains("add_continuous_aggregate_policy('$viewName'"),
                "Expected refresh policy for continuous aggregate: $viewName",
            )
        }
    }

    @Test
    fun `timescaledb dashboard separates raw and continuous aggregate comparison panels`() {
        val dashboardText = Files.readString(projectRoot.resolve("infra/grafana/dashboards/api-stats-timescaledb.json"))
        val dashboard = objectMapper.readTree(dashboardText)
        val panelTitles = dashboard.path("panels").elements().asSequence()
            .map { it.path("title").asText() }
            .toSet()

        listOf(
            "Raw API Key Calls - Daily",
            "Continuous Aggregate API Key Calls - Daily",
            "Raw API Key Calls - Monthly",
            "Continuous Aggregate API Key Calls - Monthly",
            "Raw API Key Calls - Yearly",
            "Continuous Aggregate API Key Calls - Yearly",
        ).forEach { title ->
            assertTrue(panelTitles.contains(title), "Expected dashboard panel: $title")
        }

        assertTrue(dashboardText.contains("FROM api_call_events"), "Expected raw hypertable panels")
        assertTrue(dashboardText.contains("FROM api_key_call_stats_daily"), "Expected daily continuous aggregate panel")
        assertTrue(dashboardText.contains("FROM api_key_call_stats_monthly"), "Expected monthly continuous aggregate panel")
        assertTrue(dashboardText.contains("FROM api_key_call_stats_yearly"), "Expected yearly continuous aggregate panel")
    }

    private fun readYaml(path: Path): Map<String, Any?> {
        assertTrue(Files.exists(path), "Expected config file to exist: $path")
        @Suppress("UNCHECKED_CAST")
        return yaml.load<Map<String, Any?>>(Files.readString(path))
    }

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any?>.map(key: String): Map<String, Any?> =
        assertNotNull(this[key] as? Map<String, Any?>, "Expected '$key' to be a map")

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any?>.list(key: String): List<Any?> =
        assertNotNull(this[key] as? List<Any?>, "Expected '$key' to be a list")

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any?>.listOfMaps(key: String): List<Map<String, Any?>> =
        assertNotNull(this[key] as? List<Map<String, Any?>>, "Expected '$key' to be a list of maps")
}
