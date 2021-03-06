package org.springframework.security.dsl.config.builders.server

import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.dsl.config.builders.test.SpringTestRule
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler
import org.springframework.security.web.server.csrf.ServerCsrfTokenRepository
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.config.EnableWebFlux

/**
 * Tests for [ServerCsrfDsl]
 *
 * @author Eleftheria Stein
 */
internal class ServerCsrfDslTests {
    @Rule
    @JvmField
    val spring = SpringTestRule()

    private lateinit var client: WebTestClient

    @Autowired
    fun setup(context: ApplicationContext) {
        this.client = WebTestClient
                .bindToApplicationContext(context)
                .configureClient()
                .build()
    }

    @Test
    fun `post when CSRF protection enabled then requires CSRF token`() {
        this.spring.register(CsrfConfig::class.java).autowire()

        this.client.post()
                .uri("/")
                .exchange()
                .expectStatus().isForbidden
    }

    @EnableWebFluxSecurity
    @EnableWebFlux
    class CsrfConfig {
        @Bean
        fun springWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
            return http {
                csrf { }
            }
        }
    }

    @Test
    fun `post when CSRF protection disabled then CSRF token is not required`() {
        this.spring.register(CsrfDisabledConfig::class.java).autowire()

        this.client.post()
                .uri("/")
                .exchange()
                .expectStatus().isOk
    }

    @EnableWebFluxSecurity
    @EnableWebFlux
    class CsrfDisabledConfig {
        @Bean
        fun springWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
            return http {
                csrf {
                    disable()
                }
            }
        }

        @RestController
        internal class TestController {
            @PostMapping("/")
            fun home() {
            }
        }
    }

    @Test
    fun `post when request matches CSRF matcher then CSRF token required`() {
        this.spring.register(CsrfMatcherConfig::class.java).autowire()

        this.client.post()
                .uri("/csrf")
                .exchange()
                .expectStatus().isForbidden
    }

    @Test
    fun `post when request does not match CSRF matcher then CSRF token is not required`() {
        this.spring.register(CsrfMatcherConfig::class.java).autowire()

        this.client.post()
                .uri("/")
                .exchange()
                .expectStatus().isOk
    }

    @EnableWebFluxSecurity
    @EnableWebFlux
    class CsrfMatcherConfig {
        @Bean
        fun springWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
            return http {
                csrf {
                    requireCsrfProtectionMatcher = PathPatternParserServerWebExchangeMatcher("/csrf")
                }
            }
        }

        @RestController
        internal class TestController {
            @PostMapping("/")
            fun home() {
            }

            @PostMapping("/csrf")
            fun csrf() {
            }
        }
    }

    @Test
    fun `csrf when custom access denied handler then handler used`() {
        this.spring.register(CustomAccessDeniedHandlerConfig::class.java).autowire()

        this.client.post()
                .uri("/")
                .exchange()

        Mockito.verify<ServerAccessDeniedHandler>(CustomAccessDeniedHandlerConfig.ACCESS_DENIED_HANDLER)
                .handle(any(), any())
    }

    @EnableWebFluxSecurity
    @EnableWebFlux
    class CustomAccessDeniedHandlerConfig {
        companion object {
            var ACCESS_DENIED_HANDLER: ServerAccessDeniedHandler = mock(ServerAccessDeniedHandler::class.java)
        }

        @Bean
        fun springWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
            return http {
                csrf {
                    accessDeniedHandler = ACCESS_DENIED_HANDLER
                }
            }
        }
    }

    @Test
    fun `csrf when custom token repository then repository used`() {
        this.spring.register(CustomCsrfTokenRepositoryConfig::class.java).autowire()

        this.client.post()
                .uri("/")
                .exchange()

        Mockito.verify<ServerCsrfTokenRepository>(CustomCsrfTokenRepositoryConfig.TOKEN_REPOSITORY)
                .loadToken(any())
    }

    @EnableWebFluxSecurity
    @EnableWebFlux
    class CustomCsrfTokenRepositoryConfig {
        companion object {
            var TOKEN_REPOSITORY: ServerCsrfTokenRepository = mock(ServerCsrfTokenRepository::class.java)
        }

        @Bean
        fun springWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
            return http {
                csrf {
                    csrfTokenRepository = TOKEN_REPOSITORY
                }
            }
        }
    }
}