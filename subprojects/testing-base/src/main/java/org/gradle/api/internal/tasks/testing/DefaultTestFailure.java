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

package org.gradle.api.internal.tasks.testing;

import org.gradle.api.tasks.testing.TestFailure;

public class DefaultTestFailure implements TestFailure {

    private final Throwable rawFailure;
    private final boolean isAssertionFailure;
    private final String expected;
    private final String actual;
    private final String message;
    private final String stacktrace;

    public DefaultTestFailure(Throwable rawFailure, boolean isAssertionFailure, String expected, String actual, String message, String stacktrace) {
        this.expected = expected;
        this.actual = actual;
        if (rawFailure == null) {
            throw new RuntimeException(new IllegalAccessException("This should not happen"));
        }
        this.rawFailure = rawFailure;
        this.isAssertionFailure = isAssertionFailure;
        this.message = message;
        this.stacktrace = stacktrace;
    }

    @Override
    public Throwable getRawFailure() {
        return rawFailure;
    }

    @Override
    public boolean isAssertionFailure() {
        return isAssertionFailure;
    }

    @Override
    public String getExpected() {
        return expected;
    }

    @Override
    public String getActual() {
        return actual;
    }

    public String getMessage() {
        return message;
    }

    public String getStacktrace() {
        return stacktrace;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultTestFailure that = (DefaultTestFailure) o;

        if (isAssertionFailure != that.isAssertionFailure) {
            return false;
        }
        if (rawFailure != null ? !rawFailure.equals(that.rawFailure) : that.rawFailure != null) {
            return false;
        }
        if (expected != null ? !expected.equals(that.expected) : that.expected != null) {
            return false;
        }
        if (actual != null ? !actual.equals(that.actual) : that.actual != null) {
            return false;
        }
        if (message != null ? !message.equals(that.message) : that.message != null) {
            return false;
        }
        return stacktrace != null ? stacktrace.equals(that.stacktrace) : that.stacktrace == null;
    }

    @Override
    public int hashCode() {
        int result = rawFailure != null ? rawFailure.hashCode() : 0;
        result = 31 * result + (isAssertionFailure ? 1 : 0);
        result = 31 * result + (expected != null ? expected.hashCode() : 0);
        result = 31 * result + (actual != null ? actual.hashCode() : 0);
        result = 31 * result + (message != null ? message.hashCode() : 0);
        result = 31 * result + (stacktrace != null ? stacktrace.hashCode() : 0);
        return result;
    }
}
