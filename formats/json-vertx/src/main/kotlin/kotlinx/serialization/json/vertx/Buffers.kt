/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.vertx

import io.vertx.core.buffer.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.json.internal.*
import kotlinx.serialization.json.vertx.internal.*

// copied and adapted from "formats/json-io/commonMain/src/kotlinx/serialization/json/io/IoStreams.kt"

/**
 * Serializes the [value] with [serializer] into a [buffer] using JSON format and UTF-8 encoding.
 *
 * @throws [SerializationException] if the given value cannot be serialized to JSON.
 */
@ExperimentalSerializationApi
public fun <T> Json.encodeToBuffer(
    serializer: SerializationStrategy<T>,
    value: T,
    buffer: Buffer
) {
    val writer = JsonToBufferWriter(buffer)
    try {
        encodeByWriter(this, writer, serializer, value)
    } finally {
        writer.release()
    }
}

/**
 * Serializes given [value] to a [buffer] using UTF-8 encoding and serializer retrieved from the reified type parameter.
 *
 * @throws [SerializationException] if the given value cannot be serialized to JSON.
 */
@ExperimentalSerializationApi
public inline fun <reified T> Json.encodeToBuffer(
    value: T,
    buffer: Buffer
): Unit = encodeToBuffer(serializersModule.serializer(), value, buffer)


// TODO make the functions below public when `BufferSerialReader` is fully implemented

/**
 * Deserializes JSON from [buffer] using UTF-8 encoding to a value of type [T] using [deserializer].
 *
 * Note that this functions expects that exactly one object would be present in the source
 * and throws an exception if there are any dangling bytes after an object.
 *
 * @throws [SerializationException] if the given JSON input cannot be deserialized to the value of type [T].
 * @throws [IndexOutOfBoundsException] If the buffer doesn't have enough bytes to read.
 */
@ExperimentalSerializationApi
private fun <T> Json.decodeFromBuffer(
    deserializer: DeserializationStrategy<T>,
    buffer: Buffer
): T {
    return decodeByReader(this, deserializer, BufferSerialReader(buffer))
}

/**
 * Deserializes the contents of given [buffer] to the value of type [T] using UTF-8 encoding and
 * deserializer retrieved from the reified type parameter.
 *
 * Note that this functions expects that exactly one object would be present in the stream
 * and throws an exception if there are any dangling bytes after an object.
 *
 * @throws [SerializationException] if the given JSON input cannot be deserialized to the value of type [T].
 * @throws [IndexOutOfBoundsException] If the buffer doesn't have enough bytes to read.
 */
@ExperimentalSerializationApi
private inline fun <reified T> Json.decodeFromBuffer(buffer: Buffer): T =
    decodeFromBuffer(serializersModule.serializer(), buffer)


/**
 * Transforms the given [buffer] into lazily deserialized sequence of elements of type [T] using UTF-8 encoding and [deserializer].
 * Unlike [decodeFromBuffer], [buffer] is allowed to have more than one element, separated as [format] declares.
 *
 * Elements must all be of type [T].
 * Elements are parsed lazily when resulting [Sequence] is evaluated.
 * Resulting sequence is tied to the stream and can be evaluated only once.
 *
 * **Resource caution:** this method neither closes the [buffer] when the parsing is finished nor provides a method to close it manually.
 * It is a caller responsibility to hold a reference to a source and close it. Moreover, because source is parsed lazily,
 * closing it before returned sequence is evaluated completely will result in [Exception] from decoder.
 *
 * @throws [SerializationException] if the given JSON input cannot be deserialized to the value of type [T].
 * @throws [IndexOutOfBoundsException] If the buffer doesn't have enough bytes to read.
 */
@ExperimentalSerializationApi
private fun <T> Json.decodeSourceToSequence(
    buffer: Buffer,
    deserializer: DeserializationStrategy<T>,
    format: DecodeSequenceMode = DecodeSequenceMode.AUTO_DETECT
): Sequence<T> {
    return decodeToSequenceByReader(this, BufferSerialReader(buffer), deserializer, format)
}

/**
 * Transforms the given [buffer] into lazily deserialized sequence of elements of type [T] using UTF-8 encoding and deserializer retrieved from the reified type parameter.
 * Unlike [decodeSourceToSequence], [buffer] is allowed to have more than one element, separated as [format] declares.
 *
 * Elements must all be of type [T].
 * Elements are parsed lazily when resulting [Sequence] is evaluated.
 * Resulting sequence is tied to the stream and constrained to be evaluated only once.
 *
 * **Resource caution:** this method does not close [buffer] when the parsing is finished neither provides method to close it manually.
 * It is a caller responsibility to hold a reference to a source and close it. Moreover, because source is parsed lazily,
 * closing it before returned sequence is evaluated fully would result in [Exception] from decoder.
 *
 * @throws [SerializationException] if the given JSON input cannot be deserialized to the value of type [T].
 * @throws [IndexOutOfBoundsException] If the buffer doesn't have enough bytes to read.
 */
@ExperimentalSerializationApi
private inline fun <reified T> Json.decodeSourceToSequence(
    buffer: Buffer,
    format: DecodeSequenceMode = DecodeSequenceMode.AUTO_DETECT
): Sequence<T> = decodeSourceToSequence(buffer, serializersModule.serializer(), format)
