package com.example.oidccommon.security

import com.example.oidccommon.config.OidcSecurityProperties
import com.example.sessioncommon.security.SessionAppTaggingFilter
import org.springframework.http.HttpMethod
import org.springframework.boot.autoconfigure.security.servlet.PathRequest
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.context.SecurityContextHolderFilter
import org.springframework.stereotype.Component

@Component
class OidcSecurityConfigurer(
    private val oidcSecurityProperties: OidcSecurityProperties,
    private val keycloakOidcUserService: KeycloakOidcUserService,
    private val keycloakLogoutSuccessHandler: KeycloakLogoutSuccessHandler,
    private val sessionAppTaggingFilter: SessionAppTaggingFilter,
) {

    fun build(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                    .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico").permitAll()
                    .requestMatchers("/", "/public", "/error", "/actuator/health", "/oauth2/**", "/login/**", "/logout", "/access-denied").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/admin/**").hasAnyAuthority(*oidcSecurityProperties.adminAuthorities())
                    .requestMatchers("/api/**").hasAnyAuthority(*oidcSecurityProperties.accessAuthorities())
                    .anyRequest().hasAnyAuthority(*oidcSecurityProperties.accessAuthorities())
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
                    .deleteCookies(oidcSecurityProperties.logoutCookieName)
            }
            .csrf { csrf ->
                csrf.ignoringRequestMatchers("/api/admin/**")
            }
            .exceptionHandling { exceptionHandling ->
                exceptionHandling.accessDeniedPage("/access-denied")
            }
            .oauth2Client(Customizer.withDefaults())
            .addFilterAfter(sessionAppTaggingFilter, SecurityContextHolderFilter::class.java)

        return http.build()
    }
}
