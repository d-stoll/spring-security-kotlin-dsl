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

package org.springframework.security.dsl.config.builders.util.delegates

import kotlin.properties.Delegates

/**
 * Observing delegate functions to trigger a callback function when the observed property is changed.
 *
 * @author Daniel Stoll
 * @since 5.2
 */
class CallbackDelegates {
    companion object {

        fun callOnSet(method: () -> Any?) = Delegates.observable(false) {
            _, _, new -> if (new) method.invoke()
        }

        fun <T> callOnSet(method: (T) -> Any?) = Delegates.observable<T?>(null) {
            _, _, new: T? -> new?.let { method.invoke(new) }
        }

    }
}