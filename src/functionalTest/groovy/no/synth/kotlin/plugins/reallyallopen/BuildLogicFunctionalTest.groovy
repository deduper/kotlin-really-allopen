package no.synth.kotlin.plugins.reallyallopen

import groovy.util.slurpersupport.GPathResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class BuildLogicFunctionalTest extends Specification {
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

    def 'should include plugin via plugins block'() {

        given:
        settingsFile << 'rootProject.name = "kotlin-really-allopen-functional-test"'
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

    GradleRunner runGradle( ){
        return GradleRunner.create()
        .withProjectDir(testProjectDir.root)
        .withArguments('build')
        .withPluginClasspath()
        .forwardOutput()
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
                thrown(CannotCreateMockException)
            }

            def "final function should be open"() {
              when:
                def mockedClass = Mock(SomeFinalClass)
                1 * mockedClass.someFinalMethod() >> "mock cheese"

              then:
                thrown(CannotCreateMockException)
            }
        }
    """

    static someFinalClass = """
        class SomeFinalClass {
            fun someFinalMethod() = "cheese"
        }
    """

    static someAnnotatedClass = """
        annotation class OpenItUp

        @OpenItUp
        class SomeFinalClass {
            fun someFinalMethod() = "cheese"
        }
    """
}