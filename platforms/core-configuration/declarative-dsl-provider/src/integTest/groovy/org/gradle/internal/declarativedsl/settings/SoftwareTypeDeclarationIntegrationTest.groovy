/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.internal.declarativedsl.settings

import groovy.test.NotYetImplemented
import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.test.fixtures.server.http.MavenHttpPluginRepository
import org.hamcrest.Matchers
import org.junit.Rule

import static org.gradle.util.Matchers.containsText

class SoftwareTypeDeclarationIntegrationTest extends AbstractIntegrationSpec implements SoftwareTypeFixture {
    @Rule
    MavenHttpPluginRepository pluginPortal = MavenHttpPluginRepository.asGradlePluginPortal(executer, mavenRepo)

    @Rule
    MavenHttpPluginRepository mavenHttpRepo = new MavenHttpPluginRepository(mavenRepo)

    def 'can declare and configure a custom software type from included build'() {
        given:
        withSoftwareTypePlugins().prepareToExecute()

        file("settings.gradle.something") << pluginsFromIncludedBuild

        file("build.gradle.something") << declarativeScriptThatConfiguresOnlyTestSoftwareType

        when:
        run(":printTestSoftwareTypeExtensionConfiguration")

        then:
        assertThatDeclaredValuesAreSetProperly()

        and:
        outputContains("Applying SoftwareTypeImplPlugin")
        outputDoesNotContain("Applying AnotherSoftwareTypeImplPlugin")
    }

    def 'can declare and configure a custom software type from published plugin'() {
        given:
        def pluginBuilder = withSoftwareTypePlugins()
        pluginBuilder.publishAs("com", "example", "1.0", pluginPortal, createExecuter()).allowAll()

        file("settings.gradle.something") << """
            plugins {
                id("com.example.test-software-type").version("1.0")
            }
        """

        file("build.gradle.something") << declarativeScriptThatConfiguresOnlyTestSoftwareType

        when:
        run(":printTestSoftwareTypeExtensionConfiguration")

        then:
        assertThatDeclaredValuesAreSetProperly()

        and:
        outputContains("Applying SoftwareTypeImplPlugin")
        outputDoesNotContain("Applying AnotherSoftwareTypeImplPlugin")
    }

    /**
     * This test is not yet implemented because it requires a custom repository to be set up which is not possible yet with the declarative dsl.
     */
    @NotYetImplemented
    def 'can declare and configure a custom software type from plugin published to a custom repository'() {
        given:
        def pluginBuilder = withSoftwareTypePlugins()
        pluginBuilder.publishAs("com", "example", "1.0", mavenHttpRepo, createExecuter()).allowAll()

        file("settings.gradle.something") << """
            pluginManagement {
                repositories {
                    maven { url("$mavenHttpRepo.uri") }
                }
            }
            plugins {
                id("com.example.test-software-type").version("1.0")
            }
        """

        file("build.gradle.something") << declarativeScriptThatConfiguresOnlyTestSoftwareType

        when:
        run(":printTestSoftwareTypeExtensionConfiguration")

        then:
        assertThatDeclaredValuesAreSetProperly()

        and:
        outputContains("Applying SoftwareTypeImplPlugin")
        outputDoesNotContain("Applying AnotherSoftwareTypeImplPlugin")
    }

    def 'can declare multiple custom software types from a single settings plugin'() {
        given:
        withSettingsPluginThatExposesMultipleSoftwareTypes().prepareToExecute()

        file("settings.gradle.something") << pluginsFromIncludedBuild

        file("build.gradle.something") << declarativeScriptThatConfiguresOnlyTestSoftwareType + """
            anotherSoftwareType {
                id = "test2"

                foo {
                    bar = "fizz"
                }
            }
        """

        when:
        run(":printTestSoftwareTypeExtensionConfiguration", ":printAnotherSoftwareTypeExtensionConfiguration")

        then:
        assertThatDeclaredValuesAreSetProperly()
        outputContains("""id = test2\nbar = fizz""")
    }

    def 'can declare multiple custom software types from a single settings plugin but apply only one'() {
        given:
        withSettingsPluginThatExposesMultipleSoftwareTypes().prepareToExecute()

        file("settings.gradle.something") << pluginsFromIncludedBuild

        file("build.gradle.something") << declarativeScriptThatConfiguresOnlyTestSoftwareType

        when:
        run(":printTestSoftwareTypeExtensionConfiguration")

        then:
        assertThatDeclaredValuesAreSetProperly()

        and:
        outputContains("Applying SoftwareTypeImplPlugin")
        outputDoesNotContain("Applying AnotherSoftwareTypeImplPlugin")
    }

    def 'can declare and configure a custom software type with different public and implementation model types'() {
        given:
        withSoftwareTypePluginThatHasDifferentPublicAndImplementationModelTypes().prepareToExecute()

        file("settings.gradle.something") << pluginsFromIncludedBuild

        file("build.gradle.something") << declarativeScriptThatConfiguresOnlyTestSoftwareType

        when:
        run(":printTestSoftwareTypeExtensionImplConfiguration")

        then:
        assertThatDeclaredValuesAreSetProperly()

        when:
        file("build.gradle.something") << """
            testSoftwareType {
                nonPublic = "foo"
            }
        """
        fails(":printTestSoftwareTypeExtensionImplConfiguration")

        then:
        failure.assertThatCause(Matchers.containsString("Failed to interpret the declarative DSL file"))
        failure.assertThatCause(Matchers.containsString("unresolved reference 'nonPublic'"))
    }

    def 'can declare and configure a custom software type from a parent class'() {
        given:
        withSoftwareTypePluginThatExposesSoftwareTypeFromParentClass().prepareToExecute()

        file("settings.gradle.something") << pluginsFromIncludedBuild

        file("build.gradle.something") << declarativeScriptThatConfiguresOnlyTestSoftwareType

        when:
        run(":printTestSoftwareTypeExtensionConfiguration")

        then:
        assertThatDeclaredValuesAreSetProperly()
    }

    def 'can declare and configure a custom software type from a plugin with unannotated methods'() {
        given:
        withSoftwareTypePluginThatHasUnannotatedMethods().prepareToExecute()

        file("settings.gradle.something") << pluginsFromIncludedBuild

        file("build.gradle.something") << declarativeScriptThatConfiguresOnlyTestSoftwareType

        when:
        run(":printTestSoftwareTypeExtensionConfiguration")

        then:
        assertThatDeclaredValuesAreSetProperly()
    }

    def 'sensible error when model types do not match in software type declaration'() {
        given:
        withSoftwareTypePluginWithMismatchedModelTypes().prepareToExecute()

        file("settings.gradle.something") << pluginsFromIncludedBuild

        file("build.gradle.something") << declarativeScriptThatConfiguresOnlyTestSoftwareType

        when:
        fails(":printTestSoftwareTypeExtensionConfiguration")

        then:
        failure.assertHasCause("Failed to apply plugin class 'org.gradle.test.SoftwareTypeImplPlugin'.")
        failure.assertHasCause("A problem was found with the SoftwareTypeImplPlugin plugin.")
        failure.assertThatCause(containsText("Type 'org.gradle.test.SoftwareTypeImplPlugin_Decorated' property 'testSoftwareTypeExtension' has @SoftwareType annotation with public type 'String' used on property of type 'TestSoftwareTypeExtension'."))
    }

    def 'sensible error when a software type plugin is registered that does not expose a software type'() {
        given:
        withSoftwareTypePluginThatDoesNotExposeSoftwareTypes().prepareToExecute()

        file("settings.gradle.something") << pluginsFromIncludedBuild

        when:
        fails(":help")

        then:
        failure.assertHasCause("Failed to apply plugin 'com.example.test-software-type'.")
        failure.assertHasCause("A plugin with type 'org.gradle.test.SoftwareTypeImplPlugin' was registered as a software type plugin, but it does not expose any software types. Software type plugins must expose software types via properties with the @SoftwareType annotation.")
    }

    static String getPluginsFromIncludedBuild() {
        return """
            pluginManagement {
                includeBuild("plugins")
            }
            plugins {
                id("com.example.test-software-type")
            }
        """
    }

    static String getDeclarativeScriptThatConfiguresOnlyTestSoftwareType() {
        return """
            testSoftwareType {
                id = "test"

                foo {
                    bar = "baz"
                }
            }
        """
    }

    void assertThatDeclaredValuesAreSetProperly() {
        outputContains("""id = test\nbar = baz""")
    }
}
