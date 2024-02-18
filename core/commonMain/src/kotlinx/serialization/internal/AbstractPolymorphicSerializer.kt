/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.internal

import kotlinx.serialization.*
import kotlinx.serialization.encoding.*
import kotlin.jvm.*
import kotlin.properties.*
import kotlin.reflect.*

/**
 * Base class for providing multiplatform polymorphic serialization.
 *
 * This class cannot be implemented by library users. To learn how to use it for your case,
 * please refer to [PolymorphicSerializer] for interfaces/abstract classes and [SealedClassSerializer] for sealed classes.
 *
 * By default, without special support from [Encoder], polymorphic types are serialized as list with
 * two elements: class [serial name][SerialDescriptor.serialName] (String) and the object itself.
 * Serial name equals to fully-qualified class name by default and can be changed via @[SerialName] annotation.
 */
@InternalSerializationApi
@OptIn(ExperimentalSerializationApi::class)
public abstract class AbstractPolymorphicSerializer<T : Any> internal constructor() : KSerializer<T> {

    /**
     * Base class for all classes that this polymorphic serializer can serialize or deserialize.
     */
    public abstract val baseClass: KClass<T>

    public final override fun serialize(encoder: Encoder, value: T) {
        val actualSerializer = findPolymorphicSerializer(encoder, value)
        encoder.encodeStructure(descriptor) {
            if (descriptor.useSerialPolymorphicNumbers)
                encodeIntElement(
                    descriptor,
                    0,
                    actualSerializer.descriptor.serialPolymorphicNumberByBaseClass.getValue(baseClass)
                )
            else
                encodeStringElement(descriptor, 0, actualSerializer.descriptor.serialName)
            encodeSerializableElement(descriptor, 1, actualSerializer.cast(), value)
        }
    }

    public final override fun deserialize(decoder: Decoder): T = decoder.decodeStructure(descriptor) {
        var klassName: String? = null
        var serialPolymorphicNumber: Int? = null
        var value: Any? = null
        if (decodeSequentially()) {
            return@decodeStructure decodeSequentially(this)
        }

        mainLoop@ while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> {
                    break@mainLoop
                }

                0 -> {
                    if (descriptor.useSerialPolymorphicNumbers)
                        serialPolymorphicNumber = decodeIntElement(descriptor, index)
                    else
                        klassName = decodeStringElement(descriptor, index)
                }

                1 -> {
                    val serializer = if (descriptor.useSerialPolymorphicNumbers) {
                        requireNotNull(serialPolymorphicNumber) { "Cannot read polymorphic value before its type token" }
                        findPolymorphicSerializerWithNumber(this, serialPolymorphicNumber)
                    } else {
                        requireNotNull(klassName) { "Cannot read polymorphic value before its type token" }
                        findPolymorphicSerializer(this, klassName)
                    }
                    value = decodeSerializableElement(descriptor, index, serializer)
                }

                else -> throw SerializationException(
                    "Invalid index in polymorphic deserialization of " +
                        (klassName ?: "unknown class") +
                        "\n Expected 0, 1 or DECODE_DONE(-1), but found $index"
                )
            }
        }
        @Suppress("UNCHECKED_CAST")
        requireNotNull(value) { "Polymorphic value has not been read for class $klassName" } as T
    }

    private fun decodeSequentially(compositeDecoder: CompositeDecoder): T {
        val klassName = compositeDecoder.decodeStringElement(descriptor, 0)
        val serializer = findPolymorphicSerializer(compositeDecoder, klassName)
        return compositeDecoder.decodeSerializableElement(descriptor, 1, serializer)
    }

    /**
     * Lookups an actual serializer for given [klassName] withing the current [base class][baseClass].
     * May use context from the [decoder].
     */
    @InternalSerializationApi
    public open fun findPolymorphicSerializerOrNull(
        decoder: CompositeDecoder,
        klassName: String?
    ): DeserializationStrategy<T>? = decoder.serializersModule.getPolymorphic(baseClass, klassName)

    /**
     * TODO
     */
    @InternalSerializationApi
    public open fun findPolymorphicSerializerWithNumberOrNull(
        decoder: CompositeDecoder,
        serialPolymorphicNumber: Int?
    ): DeserializationStrategy<T>? =
        decoder.serializersModule.getPolymorphicWithNumber(baseClass, serialPolymorphicNumber)


    /**
     * Lookups an actual serializer for given [value] within the current [base class][baseClass].
     * May use context from the [encoder].
     */
    @InternalSerializationApi
    public open fun findPolymorphicSerializerOrNull(
        encoder: Encoder,
        value: T
    ): SerializationStrategy<T>? =
        encoder.serializersModule.getPolymorphic(baseClass, value)
}

@JvmName("throwSubtypeNotRegistered")
internal fun throwSubtypeNotRegistered(subClassName: String?, baseClass: KClass<*>): Nothing {
    val scope = "in the polymorphic scope of '${baseClass.simpleName}'"
    throw SerializationException(
        if (subClassName == null)
            "Class discriminator was missing and no default serializers were registered $scope."
        else
            "Serializer for subclass '$subClassName' is not found $scope.\n" +
                "Check if class with serial name '$subClassName' exists and serializer is registered in a corresponding SerializersModule.\n" +
                "To be registered automatically, class '$subClassName' has to be '@Serializable', and the base class '${baseClass.simpleName}' has to be sealed and '@Serializable'."
    )
}

@JvmName("throwSubtypeNotRegistered")
internal fun throwSubtypeNotRegistered(serialPolymorphicNumber: Int?, baseClass: KClass<*>): Nothing {
    val scope = "in the polymorphic scope of '${baseClass.simpleName}'"
    throw SerializationException(
        (
            if (serialPolymorphicNumber == null)
                "Class discriminator serial polymorphic number was missing and no default serializers were registered $scope."
            else
                "Serializer for subclass serial polymorphic number '$serialPolymorphicNumber' is not found $scope.\n" +
                    "Check if class with serial polymorphic number '$serialPolymorphicNumber' exists and serializer is registered in a corresponding SerializersModule.\n" +
                    "To be registered automatically, class annotated with '@SerialPolymorphicNumber($serialPolymorphicNumber)' has to be '@Serializable', and the base class '${baseClass.simpleName}' has to be sealed and '@Serializable'.\n"
            ) +
            "\nRemove the `@UseSerialPolymorphicNumbers` annotation from the base class `${baseClass.simpleName}` if you want to switch back to polymorphic serialization using the serial name strings."
    )
}

@JvmName("throwSubtypeNotRegistered")
internal fun throwSubtypeNotRegistered(subClass: KClass<*>, baseClass: KClass<*>): Nothing =
    throwSubtypeNotRegistered(subClass.simpleName ?: "$subClass", baseClass)
