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

import org.gradle.tooling.Failure;
import org.gradle.tooling.TestFrameworkFailure;

import javax.annotation.Nullable;
import java.util.List;

public class DefaultTestFrameworkFailure implements TestFrameworkFailure {

    private final String message;
    private final String description;
    private final List<? extends Failure> causes;
    private final Throwable rawFailure;

    public DefaultTestFrameworkFailure(String message, String description, List<? extends Failure> causes, Throwable rawFailure) {
        this.message = message;
        this.description = description;
        this.causes = causes;
        this.rawFailure = rawFailure;
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

    @Override
    public Throwable getRawFailure() {
        return rawFailure;
    }
}
