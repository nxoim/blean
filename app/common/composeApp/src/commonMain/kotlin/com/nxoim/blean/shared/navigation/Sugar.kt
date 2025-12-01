package com.nxoim.blean.shared.navigation

import com.arkivanov.decompose.GenericComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import kotlin.jvm.JvmInline

inline fun <
        reified DestinationGeneric : Any,
        ContextGeneric : GenericComponentContext<ContextGeneric>,
        InstanceGeneric : Any
> ContextGeneric.childStack(
    navigator: StackNavigation<DestinationGeneric>,
    noinline initialStack: () -> List<DestinationGeneric>,
    serializer: KSerializer<DestinationGeneric>? = serializer(),
    handleBackButton: Boolean = true,
    key: NavigatorKey = navigatorKey<DestinationGeneric>(),
    noinline childFactory: (destination: DestinationGeneric, componentContext: ContextGeneric) -> InstanceGeneric,
): Value<ChildStack<*, InstanceGeneric>> = this.childStack(
    source = navigator,
    initialStack = initialStack,
    serializer = serializer,
    handleBackButton = handleBackButton,
    key = key.value,
    childFactory = childFactory
)

inline fun <
        reified DestinationGeneric : Any,
        ContextGeneric : GenericComponentContext<ContextGeneric>,
        InstanceGeneric : Any
> ContextGeneric.childStack(
    navigator: StackNavigation<DestinationGeneric>,
    initialConfiguration: DestinationGeneric,
    serializer: KSerializer<DestinationGeneric>? = serializer(),
    handleBackButton: Boolean = true,
    key: NavigatorKey = navigatorKey<DestinationGeneric>(),
    noinline childFactory: (destination: DestinationGeneric, componentContext: ContextGeneric) -> InstanceGeneric,
): Value<ChildStack<*, InstanceGeneric>> = this.childStack(
    source = navigator,
    initialConfiguration = initialConfiguration,
    serializer = serializer,
    handleBackButton = handleBackButton,
    key = key.value,
    childFactory = childFactory
)


inline fun <reified C : Any> navigatorKey(
    additionalKey: Any = ""
) = NavigatorKey("${C::class}$additionalKey")

// to rename?
/**
 * Just a wrapper to make using plain strings less convenient
 */
@JvmInline
value class NavigatorKey(val value: String) {
    override fun toString() = value
}

inline fun <reified T : Any> Value<ChildStack<*, *>>.findInstance(): T? =
    value.findInstance()

inline fun <reified T : Any> ChildStack<*, *>.findInstance(): T? =
    items
        .find { it.instance is T }
        ?.instance as? T