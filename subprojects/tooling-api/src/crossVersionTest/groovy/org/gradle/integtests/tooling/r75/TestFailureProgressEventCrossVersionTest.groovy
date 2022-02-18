/*
 * Copyright 2022 the original author or authors.
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

package org.gradle.integtests.tooling.r75

import org.gradle.integtests.fixtures.RepoScriptBlockUtil
import org.gradle.integtests.tooling.fixture.AbstractHttpCrossVersionSpec
import org.gradle.integtests.tooling.fixture.TargetGradleVersion
import org.gradle.integtests.tooling.fixture.ToolingApiVersion
import org.gradle.tooling.TestAssertionFailure
import org.gradle.tooling.BuildException
import org.gradle.tooling.Failure
import org.gradle.tooling.ProjectConnection
import org.gradle.tooling.TestFrameworkFailure
import org.gradle.tooling.events.ProgressEvent
import org.gradle.tooling.events.ProgressListener
import org.gradle.tooling.events.test.TestFailureResult
import org.gradle.tooling.events.test.TestFinishEvent
import org.gradle.tooling.events.test.TestOperationResult

@ToolingApiVersion(">=7.5")
@TargetGradleVersion(">=7.5")
class TestFailureProgressEventCrossVersionTest extends AbstractHttpCrossVersionSpec {

    def "Running junit 5 tests"() {
        setup:
        buildFile << """
            plugins {
                id 'java-library'
            }

            ${mavenCentralRepository()}

            dependencies {
                testImplementation 'org.junit.jupiter:junit-jupiter-api:5.7.1'
                testImplementation 'org.junit.jupiter:junit-jupiter-engine:5.7.1'
            }

            test {
                useJUnitPlatform()
            }
        """

        file('src/test/java/org/gradle/JUnitJupiterTest.java') << '''
            package org.gradle;

            import org.junit.jupiter.api.Test;

            public class JUnitJupiterTest {
                @Test
                public void fail() {
                    org.junit.jupiter.api.Assertions.assertEquals("expected", "actual", "This should be the error message");
                }

                @Test
                public void fail2() {
                    throw new RuntimeException("Boom!");
                }
            }
        '''

        when:
        def progressEventCollector = new ProgressEventCollector()
        withConnection { ProjectConnection connection ->
            connection.newBuild()
                .addProgressListener(progressEventCollector)
                .forTasks('test')
                .run()
        }

        then:
        thrown(BuildException)
        progressEventCollector.failures.size() == 2
        progressEventCollector.failures.findAll { it instanceof TestAssertionFailure }.size() == 1
        progressEventCollector.failures.findAll { it instanceof TestAssertionFailure }[0].message == 'This should be the error message ==> expected: <expected> but was: <actual>'
        progressEventCollector.failures.findAll { it instanceof TestFrameworkFailure }.size() == 1
        progressEventCollector.failures.findAll { it instanceof TestFrameworkFailure }[0].message == "Boom!"
    }

    def "Running junit 4 tests"() {
        setup:
        buildFile << """
            plugins {
                id 'java-library'
            }

            ${mavenCentralRepository()}

            dependencies {
                testImplementation 'junit:junit:4.13'
            }
        """

        file('src/test/java/FailingTest.java') << '''
            public class FailingTest {

                @org.junit.Test
                public void test() {
                    assert false;
                }

                @org.junit.Test
                public void test2() {
                    throw new RuntimeException("Boom");
                }

                @org.junit.Test
                public void test3() {
                    org.junit.Assert.assertEquals("This should fail", "myExpectedValue", "myActualValue");
                }
            }
        '''

        when:
        def progressEventCollector = new ProgressEventCollector()
        withConnection { ProjectConnection connection ->
            connection.newBuild()
                .addProgressListener(progressEventCollector)
                .forTasks('test')
                .run()
        }

        then:
        thrown(BuildException)
        progressEventCollector.failures.size() == 3
        progressEventCollector.failures.findAll { it instanceof TestAssertionFailure }.size() == 2
        progressEventCollector.failures.findAll { it instanceof TestFrameworkFailure }.size() == 1
        progressEventCollector.failures.findAll { it instanceof TestAssertionFailure && it.expected == 'myExpectedValue' && it.actual == 'myActualValue' }.size() == 1
    }

    def "running spock test"() {
        setup:
        buildFile << """
            plugins {
                id('groovy')
            }

            repositories {
                ${mavenCentralRepository()}
            }
            dependencies {
                testImplementation('org.spockframework:spock-core:2.0-groovy-3.0')
            }
            test {
                useJUnitPlatform()
            }
        """
        file("src/test/groovy/MySpockTest.groovy") << """
            class MySpockTest extends spock.lang.Specification {
                def 'my test'() {
                    expect: false
                }
            }
        """
        when:
        def progressEventCollector = new ProgressEventCollector()
        withConnection { ProjectConnection connection ->
            connection.newBuild()
                .addProgressListener(progressEventCollector)
                .forTasks('test')
                .run()
        }


        then:
        thrown(BuildException)
        progressEventCollector.failures.size() == 1
        progressEventCollector.failures[0] instanceof TestAssertionFailure
    }

    def "Running testNG test"() {
        setup:
        buildFile << """
            apply plugin: 'java'
            ${RepoScriptBlockUtil.mavenCentralRepository()}
            testing {
                suites {
                    test {
                        useTestNG('7.4.0')
                    }
                }
            }
        """

        file('src/test/java/AppException.java') << 'public class AppException extends Exception {}'
        file('src/test/java/SomeTest.java') << '''
            public class SomeTest {
                @org.testng.annotations.Test
                public void pass() {}

                @org.testng.annotations.Test
                public void fail() { assert false; }

                @org.testng.annotations.Test
                public void knownError() { throw new RuntimeException("message"); }

                @org.testng.annotations.Test
                public void unknownError() throws AppException { throw new AppException(); }

                @org.testng.annotations.Test
                public void assertionError() { org.testng.Assert.assertEquals("myExpectedValue", "myActualValue"); }
            }
        '''

        when:
        def progressEventCollector = new ProgressEventCollector()
        withConnection { ProjectConnection connection ->
            connection.newBuild()
                .addProgressListener(progressEventCollector)
                .forTasks('test')
                .run()
        }

        then:
        thrown(BuildException)
        progressEventCollector.failures.size() == 4
        progressEventCollector.failures.findAll { it instanceof TestAssertionFailure }.size() == 2
        progressEventCollector.failures.findAll { it instanceof TestFrameworkFailure }.size() == 2
    }

    class ProgressEventCollector implements ProgressListener {

        public List<Failure> failures = []

        @Override
        void statusChanged(ProgressEvent event) {
            if (event instanceof TestFinishEvent) {
                TestOperationResult result = ((TestFinishEvent) event).getResult();
                if (result instanceof TestFailureResult) {
                    failures += ((TestFailureResult)result).failures
                }
            }
        }
    }
}
