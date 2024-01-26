/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlin.test.*

class PolymorphicWithInferredTypeParameterTest : JsonTestBase() {
    @Serializable
    abstract class Response<out T>

    @Serializable
    @SerialName("OkResponse")
    data class OkResponse<out T>(val data: T) : Response<T>()

    val responseModule = SerializersModule {
        polymorphic<Response<String>> {
            subclass(serializer<OkResponse<String>>())
        }
    }

    val format = Json { serializersModule = responseModule }

    @Test
    fun test() = parametrizedTest {
        val response: Response<String> = OkResponse("kotlin")
        assertEquals("{\"type\":\"OkResponse\",\"data\":\"kotlin\"}", format.encodeToString(response))
    }
}