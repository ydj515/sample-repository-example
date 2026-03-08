package com.example.oidcsimpleexample.security

import com.example.oidcsimpleexample.config.AppSecurityProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper
import org.springframework.security.web.SecurityFilterChain
import java.net.URI

@Configuration
@EnableMethodSecurity
class SecurityConfig(
    private val appSecurityProperties: AppSecurityProperties,
    private val keycloakOidcUserService: KeycloakOidcUserService,
) {

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        keycloakLogoutSuccessHandler: KeycloakLogoutSuccessHandler,
    ): SecurityFilterChain {
        http
            .authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers("/", "/public", "/error", "/actuator/health").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/admin/**").hasRole("admin")
                    .requestMatchers("/api/**").authenticated()
                    .anyRequest().authenticated()
            }
            .oauth2Login { oauth2 ->
                oauth2.userInfoEndpoint { userInfo ->
                    userInfo.oidcUserService(keycloakOidcUserService::loadUser)
                }
            }
            .logout { logout ->
                logout
                    .logoutSuccessHandler(keycloakLogoutSuccessHandler)
                    .invalidateHttpSession(true)
                    .deleteCookies("SESSION")
            }
            .csrf { csrf ->
                csrf.ignoringRequestMatchers("/api/admin/**")
            }
            .oauth2Client(Customizer.withDefaults())

        return http.build()
    }

    @Bean
    fun keycloakLogoutSuccessHandler(): KeycloakLogoutSuccessHandler {
        return KeycloakLogoutSuccessHandler(URI.create(appSecurityProperties.endSessionUri))
    }

    @Bean
    fun grantedAuthoritiesMapper(): GrantedAuthoritiesMapper = GrantedAuthoritiesMapper { authorities -> authorities }
}
