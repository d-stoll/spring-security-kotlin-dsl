/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.security.dsl.config.builders.servlet.session

import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.mock.web.MockHttpSession
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.dsl.config.builders.servlet.invoke
import org.springframework.security.dsl.config.builders.test.SpringTestRule
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders

/**
 * Tests for [SessionFixationDsl]
 *
 * @author Eleftheria Stein
 */
class SessionFixationDslTest {
    @Rule
    @JvmField
    var spring = SpringTestRule()

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun sessionFixationWhenStrategyIsNewSessionThenNewSessionCreatedAndAttributesAreNotPreserved() {
        this.spring.register(NewSessionConfig::class.java, UserDetailsConfig::class.java).autowire()
        val givenSession = MockHttpSession()
        val givenSessionId = givenSession.id
        givenSession.clearAttributes()
        givenSession.setAttribute("name", "value")

        val result = this.mockMvc.perform(MockMvcRequestBuilders.get("/")
                .with(httpBasic("user", "password"))
                .session(givenSession))
                .andReturn()

        val resultingSession = result.request.getSession(false)
        assertThat(resultingSession).isNotEqualTo(givenSession)
        assertThat(resultingSession!!.id).isNotEqualTo(givenSessionId)
        assertThat(resultingSession.getAttribute("name")).isNull()
    }

    @EnableWebSecurity
    class NewSessionConfig : WebSecurityConfigurerAdapter() {
        override fun configure(http: HttpSecurity) {
            http {
                sessionManagement {
                    sessionFixation {
                        newSession()
                    }
                }
                httpBasic { }
            }
        }
    }

    @Test
    fun sessionFixationWhenStrategyIsMigrateSessionThenNewSessionCreatedAndAttributesArePreserved() {
        this.spring.register(MigrateSessionConfig::class.java, UserDetailsConfig::class.java).autowire()
        val givenSession = MockHttpSession()
        val givenSessionId = givenSession.id
        givenSession.clearAttributes()
        givenSession.setAttribute("name", "value")

        val result = this.mockMvc.perform(MockMvcRequestBuilders.get("/")
                .with(httpBasic("user", "password"))
                .session(givenSession))
                .andReturn()

        val resultingSession = result.request.getSession(false)
        assertThat(resultingSession).isNotEqualTo(givenSession)
        assertThat(resultingSession!!.id).isNotEqualTo(givenSessionId)
        assertThat(resultingSession.getAttribute("name")).isEqualTo("value")
    }

    @EnableWebSecurity
    class MigrateSessionConfig : WebSecurityConfigurerAdapter() {
        override fun configure(http: HttpSecurity) {
            http {
                sessionManagement {
                    sessionFixation {
                        migrateSession()
                    }
                }
                httpBasic { }
            }
        }
    }

    @Test
    fun sessionFixationWhenStrategyIsChangeSessionIdThenSessionIdChangesAndAttributesPreserved() {
        this.spring.register(ChangeSessionIdConfig::class.java, UserDetailsConfig::class.java).autowire()
        val givenSession = MockHttpSession()
        val givenSessionId = givenSession.id
        givenSession.clearAttributes()
        givenSession.setAttribute("name", "value")

        val result = this.mockMvc.perform(MockMvcRequestBuilders.get("/")
                .with(httpBasic("user", "password"))
                .session(givenSession))
                .andReturn()

        val resultingSession = result.request.getSession(false)
        assertThat(resultingSession).isEqualTo(givenSession)
        assertThat(resultingSession!!.id).isNotEqualTo(givenSessionId)
        assertThat(resultingSession.getAttribute("name")).isEqualTo("value")
    }

    @EnableWebSecurity
    class ChangeSessionIdConfig : WebSecurityConfigurerAdapter() {
        override fun configure(http: HttpSecurity) {
            http {
                sessionManagement {
                    sessionFixation {
                        changeSessionId()
                    }
                }
                httpBasic { }
            }
        }
    }

    @Test
    fun sessionFixationWhenStrategyIsNoneThenSessionDoesNotChange() {
        this.spring.register(NoneConfig::class.java, UserDetailsConfig::class.java).autowire()
        val givenSession = MockHttpSession()
        val givenSessionId = givenSession.id
        givenSession.clearAttributes()
        givenSession.setAttribute("name", "value")

        val result = this.mockMvc.perform(MockMvcRequestBuilders.get("/")
                .with(httpBasic("user", "password"))
                .session(givenSession))
                .andReturn()

        val resultingSession = result.request.getSession(false)
        assertThat(resultingSession).isEqualTo(givenSession)
        assertThat(resultingSession!!.id).isEqualTo(givenSessionId)
        assertThat(resultingSession.getAttribute("name")).isEqualTo("value")
    }

    @EnableWebSecurity
    class NoneConfig : WebSecurityConfigurerAdapter() {
        override fun configure(http: HttpSecurity) {
            http {
                sessionManagement {
                    sessionFixation {
                        none()
                    }
                }
                httpBasic { }
            }
        }
    }

    @Configuration
    class UserDetailsConfig {
        @Bean
        fun userDetailsService(): UserDetailsService {
            val userDetails = User.withDefaultPasswordEncoder()
                    .username("user")
                    .password("password")
                    .roles("USER")
                    .build()
            return InMemoryUserDetailsManager(userDetails)
        }
    }
}
