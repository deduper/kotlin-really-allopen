package no.synth.kotlin.plugins.reallyallopen

import groovy.util.slurpersupport.GPathResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class FunctionalPluginTest extends Specification {
    @Rule
    TemporaryFolder testProjectDir = new TemporaryFolder();

    static kotlinVersion = "1.3.31"
    static pluginVersion = ReallyAllOpenPluginKt.version

    File settingsFile
    File buildFile
    File testFile
    File classFile
    File testResult

    def setup() {
        settingsFile = testProjectDir.newFile('settings.gradle.kts')
        buildFile = testProjectDir.newFile('build.gradle.kts')
        testFile = new File(testProjectDir.newFolder('src', 'test', 'groovy'), 'PluginTest.groovy')
        classFile = new File(testProjectDir.newFolder('src', 'main', 'kotlin'), 'SomeFinalClass.kt')
        testResult = new File(testProjectDir.root, "build/test-results/test/TEST-PluginTest.xml")
    }

    def cleanup() {
        testResult.delete()
    }

    def 'plugin version should be real'() {
      expect:
        pluginVersion != null
        pluginVersion.size() >= 3
        pluginVersion.contains('.')
    }

    def 'should include plugin via buildscript classpath'() {
      given:
        buildFile << """
            buildscript {
                repositories {
                    mavenLocal()
                }
                dependencies {
                    classpath("no.synth.kotlin.plugins:kotlin-really-allopen:$pluginVersion")
                }
            }
            plugins {
                kotlin("jvm") version "$kotlinVersion"
                groovy
            }
            apply(plugin = "no.synth.kotlin.plugins.kotlin-really-allopen")
            
            repositories {
                mavenLocal()
                jcenter()
            }
            $dependencies
        """
        testFile << pluginTest
        classFile << someFinalClass

      when:
        def result = runGradle().build()
        def xml = testResults()

      then:
        result.task(":test").outcome == TaskOutcome.SUCCESS
        xml.@tests == 2
        xml.@failures == 0
        xml.@errors == 0
    }

    def 'should include plugin via plugins block'() {
      given:
        settingsFile << """
            pluginManagement {
                repositories {
                    mavenLocal()
                    gradlePluginPortal()
                }
            }
        """
        buildFile << """
             plugins {
                 kotlin("jvm") version "$kotlinVersion"
                 groovy
                 id("no.synth.kotlin.plugins.kotlin-really-allopen") version "$pluginVersion"
             }
             repositories {
                 mavenLocal()
                 jcenter()
             }
             $dependencies
        """
        testFile << pluginTest
        classFile << someFinalClass

      when:
        def result = runGradle().build()
        def xml = testResults()

      then:
        result.task(":test").outcome == TaskOutcome.SUCCESS
        xml.@tests == 2
        xml.@failures == 0
        xml.@errors == 0
    }

    def 'standard allopen plugin does not open non-annotated classes'() {
      given:
        settingsFile << """
            pluginManagement {
                repositories {
                    gradlePluginPortal()
                }
            }
        """
        buildFile << """
            plugins {
                kotlin("jvm") version "$kotlinVersion"
                groovy
                id("org.jetbrains.kotlin.plugin.allopen") version "$kotlinVersion"
            }
            repositories {
                jcenter()
            }
            $dependencies
        """
        testFile << pluginTest
        classFile << someFinalClass

      when:
        def result = runGradle().buildAndFail()
        def xml = testResults()

      then:
        result.task(":test").outcome == TaskOutcome.FAILED
        xml.@tests == 2
        xml.@failures == 2
        xml.@errors == 0
    }

    def 'standard allopen plugin needs special annotation'() {
      given:
        settingsFile << """
            pluginManagement {
                repositories {
                    gradlePluginPortal()
                }
            }
        """
        buildFile << """
            import org.jetbrains.kotlin.allopen.gradle.AllOpenExtension
            plugins {
                kotlin("jvm") version "$kotlinVersion"
                groovy
                id("org.jetbrains.kotlin.plugin.allopen") version "$kotlinVersion"
            }
            repositories {
                jcenter()
            }
            configure<AllOpenExtension> {
                annotation("OpenItUp")
            }
            $dependencies
        """
        testFile << pluginTest
        classFile << someAnnotatedClass

      when:
        def result = runGradle().build()
        def xml = testResults()

      then:
        result.task(":test").outcome == TaskOutcome.SUCCESS
        xml.@tests == 2
        xml.@failures == 0
        xml.@errors == 0
    }

    GradleRunner runGradle() {
        return GradleRunner.create().withDebug(true).withProjectDir(testProjectDir.root).withArguments("build", "-i")
    }

    GPathResult testResults() {
        return new XmlSlurper().parse(testResult)
    }

    static dependencies = """
        dependencies {
           implementation("org.jetbrains.kotlin:kotlin-stdlib")
           testImplementation("net.bytebuddy:byte-buddy:1.9.12")
           testImplementation("org.spockframework:spock-core:1.3-groovy-2.5")
        }
    """

    static pluginTest = """        
        import org.spockframework.mock.CannotCreateMockException
        import spock.lang.Specification
        
        class PluginTest extends Specification {
        
            def "final class should be open"() {
              when:
                Mock(SomeFinalClass)
        
              then:
                notThrown(CannotCreateMockException)
            }
        
            def "final function should be open"() {
              when:
                def mockedClass = Mock(SomeFinalClass)
                1 * mockedClass.someFinalMethod() >> "mock cheese"
        
              then:
                mockedClass.someFinalMethod() == "mock cheese"
            }
        }
    """

    static someFinalClass = """
        const val someConstVal = "some const val"
        val someVal = "some val"
        
        class SomeFinalClass {
            val someVal = "some val"
            var someVar = "some var"
            
            fun someFinalMethod() = "some final method"
            
            companion object {
                const val someConstVal = "some object const val"
                val someVal = "some object val"
                
                fun someStaticMethod() = "some static method"
            }
        }
        
        object SomeObject {
            val someObjectVal = "some object val"
        }
    """

    static someAnnotatedClass = """
        annotation class OpenItUp
        
        @OpenItUp
        class SomeFinalClass {
            fun someFinalMethod() = "some final method"
        }
    """

}