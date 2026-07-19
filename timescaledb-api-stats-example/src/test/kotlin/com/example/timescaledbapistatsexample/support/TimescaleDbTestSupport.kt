package com.example.timescaledbapistatsexample.support

import java.nio.file.Path
import javax.sql.DataSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import org.testcontainers.utility.MountableFile

/**
 * 통합 테스트가 공유하는 TimescaleDB 컨테이너.
 *
 * 운영과 같은 이미지에 같은 init SQL(`infra/timescaledb/init`)을 그대로 올린다.
 * 스키마 파일을 복사해서 쓰는 게 아니라 실제 파일을 마운트하므로,
 * init SQL이 깨지면 통합 테스트가 바로 실패한다.
 *
 * 컨테이너는 [container]에 처음 접근할 때 한 번만 뜨고 JVM 종료까지 재사용된다.
 * (Testcontainers의 Ryuk이 종료 시 정리한다.)
 */
object TimescaleDbTestSupport {
    private const val IMAGE = "timescale/timescaledb:2.28.2-pg17"
    private const val DB_NAME = "api_stats"
    private const val DB_USER = "api_stats"
    private const val DB_PASSWORD = "api_stats"

    private val projectRoot: Path = Path.of("").toAbsolutePath()

    val container: PostgreSQLContainer<*> by lazy {
        PostgreSQLContainer(DockerImageName.parse(IMAGE).asCompatibleSubstituteFor("postgres"))
            .withDatabaseName(DB_NAME)
            .withUsername(DB_USER)
            .withPassword(DB_PASSWORD)
            .withCopyFileToContainer(initScript("01_schema.sql"), "/docker-entrypoint-initdb.d/01_schema.sql")
            .withCopyFileToContainer(initScript("02_seed_api_clients.sql"), "/docker-entrypoint-initdb.d/02_seed_api_clients.sql")
            .apply { start() }
    }

    val dataSource: DataSource by lazy {
        DriverManagerDataSource().apply {
            setDriverClassName("org.postgresql.Driver")
            url = container.jdbcUrl
            username = container.username
            password = container.password
        }
    }

    val jdbcTemplate: NamedParameterJdbcTemplate by lazy { NamedParameterJdbcTemplate(dataSource) }

    /**
     * Docker가 없는 환경에서는 통합 테스트를 건너뛴다.
     *
     * 건너뛴 이유를 반드시 출력한다. 조용히 skip 되면 "테스트가 통과했다"는 착각을 하게 된다.
     */
    fun dockerAvailable(): Boolean =
        runCatching { DockerClientFactory.instance().isDockerAvailable }
            .onFailure { ex ->
                println("[TimescaleDbTestSupport] Docker를 쓸 수 없어 통합 테스트를 건너뜁니다: $ex")
            }
            .getOrDefault(false)
            .also { available ->
                if (!available) {
                    println(
                        "[TimescaleDbTestSupport] Docker 미감지. " +
                            "DOCKER_HOST 또는 ~/.testcontainers.properties 설정을 확인하세요.",
                    )
                }
            }

    /**
     * 이벤트를 비우고 집계까지 되돌린다.
     *
     * continuous aggregate는 materialize된 결과를 따로 들고 있어서
     * raw만 지우면 집계에 옛날 값이 남는다. 계층 순서(daily -> monthly -> yearly)대로 refresh 해야 한다.
     *
     * 주의: 전체 refresh는 materialization watermark를 현재 시점까지 밀어 올린다.
     * realtime aggregation은 watermark "이후" 구간만 실시간 계산으로 메꾸므로,
     * 이 뒤에 watermark보다 과거 시각으로 넣은 데이터는 다시 refresh 하기 전까지 집계에 안 잡힌다.
     * 그래서 집계를 읽는 테스트는 쓰기 후 [refreshAllAggregates]를 명시적으로 호출한다.
     */
    fun resetEvents() {
        jdbcTemplate.jdbcTemplate.execute("DELETE FROM api_call_events")
        refreshAllAggregates()
    }

    fun refreshAllAggregates() {
        listOf(
            "api_key_call_stats_daily",
            "api_key_call_stats_monthly",
            "api_key_call_stats_yearly",
        ).forEach { view ->
            // refresh_continuous_aggregate는 트랜잭션 안에서 돌 수 없어 개별 문장으로 실행한다.
            jdbcTemplate.jdbcTemplate.execute("CALL refresh_continuous_aggregate('$view', NULL, NULL)")
        }
    }

    private fun initScript(fileName: String): MountableFile =
        MountableFile.forHostPath(projectRoot.resolve("infra/timescaledb/init").resolve(fileName))
}
