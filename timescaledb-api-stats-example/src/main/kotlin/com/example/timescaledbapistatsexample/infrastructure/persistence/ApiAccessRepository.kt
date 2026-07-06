package com.example.timescaledbapistatsexample.infrastructure.persistence

import com.example.timescaledbapistatsexample.domain.model.ApiAccessSnapshot
import com.example.timescaledbapistatsexample.domain.model.ApiClient
import com.example.timescaledbapistatsexample.domain.model.ApiRoute
import com.example.timescaledbapistatsexample.domain.port.ApiAccessSnapshotProvider
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class ApiAccessRepository(
    private val jdbcTemplate: JdbcTemplate,
) : ApiAccessSnapshotProvider {
    override fun loadSnapshot(): ApiAccessSnapshot {
        val clients = jdbcTemplate.query(
            "SELECT id, name, api_key_hash, enabled FROM api_clients WHERE enabled = true",
        ) { rs, _ ->
            ApiClient(
                id = rs.getLong("id"),
                name = rs.getString("name"),
                apiKeyHash = rs.getString("api_key_hash"),
                enabled = rs.getBoolean("enabled"),
            )
        }

        val routes = jdbcTemplate.query(
            "SELECT id, method, path_pattern, description FROM api_routes",
        ) { rs, _ ->
            ApiRoute(
                id = rs.getLong("id"),
                method = rs.getString("method"),
                pathPattern = rs.getString("path_pattern"),
                description = rs.getString("description"),
            )
        }

        val permissions = jdbcTemplate.query(
            "SELECT api_client_id, api_route_id FROM api_client_route_permissions",
        ) { rs, _ ->
            rs.getLong("api_client_id") to rs.getLong("api_route_id")
        }

        return ApiAccessSnapshot(
            clientsByApiKeyHash = clients.associateBy { it.apiKeyHash },
            routes = routes,
            routeIdsByClientId = permissions.groupBy({ it.first }, { it.second }).mapValues { it.value.toSet() },
        )
    }
}
