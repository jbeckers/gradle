/*
 * Copyright 2015 the original author or authors.
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

package org.gradle.tooling.internal.consumer;

import org.gradle.tooling.TestAssertionFailure;
import org.gradle.tooling.Failure;

import javax.annotation.Nullable;
import java.util.List;

public class DefaultTestAssertionFailure implements TestAssertionFailure {

    private final String message;
    private final String description;
    private final List<? extends Failure> causes;

    private final String expected;
    private final String actual;
    private final String stacktrace;

    public DefaultTestAssertionFailure(String message, String description, String expected, String actual, List<? extends Failure> causes, String stacktrace) {
        this.message = message;
        this.description = description;
        this.causes = causes;
        this.expected = expected;
        this.actual = actual;
        this.stacktrace = stacktrace;
    }

    @Override
    public String getExpected() {
        return expected;
    }

    @Override
    public String getActual() {
        return actual;
    }

    @Nullable
    @Override
    public String getStacktrace() {
        return stacktrace;
    }

    @Nullable
    @Override
    public String getMessage() {
        return message;
    }

    @Nullable
    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public List<? extends Failure> getCauses() {
        return causes;
    }
}
