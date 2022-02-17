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

package org.gradle.api.tasks.testing;

import javax.annotation.Nullable;

/**
 * Holds serializable structural information about a test failure.
 */
public interface TestFailureDetails {

    /**
     * Returns the failure message.
     *
     * @return the failure message
     */
    String getMessage();

    /**
     * Returns the stacktrace of the failure.
     * <p/>
     * The instances are created on the test worker side allowing the clients not to deal with non-serializable exceptions.
     *
     * @return the stacktrace string
     */
    String getStacktrace();

    /**
     * Returns true if the represented failure is recognized as an assertion failure.
     *
     * @return {@code true} for assertion failures
     */
    boolean isAssertionFailure();

    /**
     * Returns a string representation of the expected value for an assertion failure.
     * <></>
     * If the current instance does not represent an assertion failure, or the test failure doesn't provide any information about expected and actual values then the method returns {@code null}.
     *
     * @return The expected value
     */
    @Nullable
    String getExpected();

    /**
     * Returns a string representation of the actual value for an assertion failure.
     * <></>
     * If the current instance does not represent an assertion failure, or the test failure doesn't provide any information about expected and actual values then the method returns {@code null}.
     *
     * @return The actual value value
     */
    @Nullable
    String getActual();
}
