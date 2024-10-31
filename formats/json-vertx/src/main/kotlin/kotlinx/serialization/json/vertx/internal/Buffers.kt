/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.vertx.internal

import io.vertx.core.buffer.*
import kotlinx.serialization.json.internal.*

// copied and adapted from "formats/json-io/commonMain/src/kotlinx/serialization/json/io/internal/IoJsonStreams.kt"

//private const val QUOTE_CHAR = '"'
private const val QUOTE_STRING = "\""

internal class JsonToBufferWriter(private val buffer: Buffer) : InternalJsonWriter {
    override fun writeLong(value: Long) {
        write(value.toString())
    }

    override fun writeChar(char: Char) {
        // TODO use `appendUtf8Char` if it becomes available someday
        buffer.appendString(char.toString())
    }

    override fun write(text: String) {
        buffer.appendString(text)
    }

    override fun writeQuoted(text: String) {
        // TODO use `appendUtf8Char` if it becomes available someday
        buffer.appendString(QUOTE_STRING)
        InternalJsonWriter.doWriteEscaping(text) { s, start, end -> buffer.appendString(s.substring(start, end)) }
        buffer.appendString(QUOTE_STRING)
    }

    override fun release() {
        // no-op
    }
}

internal class BufferSerialReader(private val buffer: Buffer) : InternalJsonReaderCodePointImpl() {
    private val bufferLength = buffer.length()
    private var bufferIndex = 0
    override fun exhausted(): Boolean = bufferIndex >= bufferLength
    override fun nextCodePoint(): Int =
        TODO("I can't find an efficient built-in method in Vert.x, Netty, or the JDK to do this.")
}
