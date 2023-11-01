package com.h0tk3y.kotlin.staticObjectNotation.demo.demoPlugins

import com.h0tk3y.kotlin.staticObjectNotation.Adding
import com.h0tk3y.kotlin.staticObjectNotation.Builder
import com.h0tk3y.kotlin.staticObjectNotation.Configuring
import com.h0tk3y.kotlin.staticObjectNotation.HasDefaultValue
import com.h0tk3y.kotlin.staticObjectNotation.Restricted

class TopLevelScope {
    @Restricted
    val plugins = PluginsBlock()

    @Configuring
    fun plugins(configure: PluginsBlock.() -> Unit) {
        configure(plugins)
    }
}

class PluginsBlock {
    private val pluginDefinitions = mutableListOf<PluginDefinition>()

    @Adding
    fun id(id: String): PluginDefinition =
        PluginDefinition(id).also(pluginDefinitions::add)
}

class PluginDefinition(@Restricted val id: String) {
    @Restricted
    var version: String = ""
    
    @HasDefaultValue
    var apply: Boolean = false

    @Builder
    infix fun version(value: String): PluginDefinition =
        apply { version = value }

    @Builder
    infix fun apply(value: Boolean): PluginDefinition =
        apply { apply = value }
}