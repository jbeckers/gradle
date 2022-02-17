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
package org.gradle.internal.build.event.types;

import org.gradle.tooling.internal.protocol.InternalFailure;
import org.gradle.tooling.internal.protocol.InternalTestFrameworkFailure;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class DefaultTestFrameworkFailure implements Serializable, InternalTestFrameworkFailure {

    private final String message;
    private final String description;
    private final InternalFailure cause;
    private final String stacktrace;

    private DefaultTestFrameworkFailure(String message, String description, InternalFailure cause, String stacktrace) {
        this.message = message;
        this.description = description;
        this.cause = cause;
        this.stacktrace = stacktrace;
    }

    public static DefaultTestFrameworkFailure fromThrowable(Throwable t, String message, String stacktrace) {
        Throwable cause = t.getCause();
        InternalFailure causeFailure = cause != null && cause != t ? DefaultFailure.fromThrowable(cause) : null;
        return new DefaultTestFrameworkFailure(message, stacktrace, causeFailure, stacktrace);
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getStacktrace() {
        return stacktrace;
    }

    @Override
    public List<? extends InternalFailure> getCauses() {
        return Collections.singletonList(cause);
    }
}
