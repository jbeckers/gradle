/*
 * Copyright 2014 the original author or authors.
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

package org.gradle.api.internal.tasks.testing.worker;

import org.gradle.api.internal.tasks.testing.DefaultNestedTestSuiteDescriptor;
import org.gradle.api.internal.tasks.testing.DefaultTestClassDescriptor;
import org.gradle.api.internal.tasks.testing.DefaultTestClassRunInfo;
import org.gradle.api.internal.tasks.testing.DefaultTestDescriptor;
import org.gradle.api.internal.tasks.testing.DefaultTestFailure;
import org.gradle.api.internal.tasks.testing.DefaultTestFailureDetails;
import org.gradle.api.internal.tasks.testing.DefaultTestMethodDescriptor;
import org.gradle.api.internal.tasks.testing.DefaultTestOutputEvent;
import org.gradle.api.internal.tasks.testing.DefaultTestSuiteDescriptor;
import org.gradle.api.internal.tasks.testing.TestCompleteEvent;
import org.gradle.api.internal.tasks.testing.TestStartEvent;
import org.gradle.api.tasks.testing.TestOutputEvent;
import org.gradle.api.tasks.testing.TestResult;
import org.gradle.internal.id.CompositeIdGenerator;
import org.gradle.internal.serialize.BaseSerializerFactory;
import org.gradle.internal.serialize.Decoder;
import org.gradle.internal.serialize.DefaultSerializerRegistry;
import org.gradle.internal.serialize.Encoder;
import org.gradle.internal.serialize.Serializer;
import org.gradle.internal.serialize.SerializerRegistry;

public class TestEventSerializer {
    public static SerializerRegistry create() {
        BaseSerializerFactory factory = new BaseSerializerFactory();
        DefaultSerializerRegistry registry = new DefaultSerializerRegistry();
        registry.register(DefaultTestClassRunInfo.class, new DefaultTestClassRunInfoSerializer());
        registry.register(CompositeIdGenerator.CompositeId.class, new IdSerializer());
        registry.register(DefaultNestedTestSuiteDescriptor.class, new DefaultNestedTestSuiteDescriptorSerializer());
        registry.register(DefaultTestSuiteDescriptor.class, new DefaultTestSuiteDescriptorSerializer());
        registry.register(WorkerTestClassProcessor.WorkerTestSuiteDescriptor.class, new WorkerTestSuiteDescriptorSerializer());
        registry.register(DefaultTestClassDescriptor.class, new DefaultTestClassDescriptorSerializer());
        registry.register(DefaultTestMethodDescriptor.class, new DefaultTestMethodDescriptorSerializer());
        registry.register(DefaultTestDescriptor.class, new DefaultTestDescriptorSerializer());
        registry.register(TestStartEvent.class, new TestStartEventSerializer());
        registry.register(TestCompleteEvent.class, new TestCompleteEventSerializer());
        registry.register(DefaultTestOutputEvent.class, new DefaultTestOutputEventSerializer());
        Serializer<Throwable> throwableSerializer = factory.getSerializerFor(Throwable.class);
        registry.register(Throwable.class, throwableSerializer);
        registry.register(DefaultTestFailure.class, new DefaultTestFailureSerializer(throwableSerializer));
        return registry;
    }

    private static class NullableSerializer<T> implements Serializer<T> {
        private final Serializer<T> serializer;

        private NullableSerializer(Serializer<T> serializer) {
            this.serializer = serializer;
        }

        @Override
        public T read(Decoder decoder) throws Exception {
            if (!decoder.readBoolean()) {
                return null;
            }
            return serializer.read(decoder);
        }

        @Override
        public void write(Encoder encoder, T value) throws Exception {
            encoder.writeBoolean(value != null);
            if (value != null) {
                serializer.write(encoder, value);
            }
        }
    }

    private static class IdSerializer implements Serializer<CompositeIdGenerator.CompositeId> {
        @Override
        public CompositeIdGenerator.CompositeId read(Decoder decoder) throws Exception {
            return new CompositeIdGenerator.CompositeId(decoder.readLong(), decoder.readLong());
        }

        @Override
        public void write(Encoder encoder, CompositeIdGenerator.CompositeId value) throws Exception {
            encoder.writeLong((Long) value.getScope());
            encoder.writeLong((Long) value.getId());
        }
    }

    private static class DefaultTestClassRunInfoSerializer implements Serializer<DefaultTestClassRunInfo> {
        @Override
        public DefaultTestClassRunInfo read(Decoder decoder) throws Exception {
            return new DefaultTestClassRunInfo(decoder.readString());
        }

        @Override
        public void write(Encoder encoder, DefaultTestClassRunInfo value) throws Exception {
            encoder.writeString(value.getTestClassName());
        }
    }

    private static class TestStartEventSerializer implements Serializer<TestStartEvent> {
        final Serializer<CompositeIdGenerator.CompositeId> idSerializer = new NullableSerializer<CompositeIdGenerator.CompositeId>(new IdSerializer());

        @Override
        public TestStartEvent read(Decoder decoder) throws Exception {
            long time = decoder.readLong();
            Object id = idSerializer.read(decoder);
            return new TestStartEvent(time, id);
        }

        @Override
        public void write(Encoder encoder, TestStartEvent value) throws Exception {
            encoder.writeLong(value.getStartTime());
            idSerializer.write(encoder, (CompositeIdGenerator.CompositeId) value.getParentId());
        }
    }

    private static class TestCompleteEventSerializer implements Serializer<TestCompleteEvent> {
        private final Serializer<TestResult.ResultType> typeSerializer = new NullableSerializer<TestResult.ResultType>(new BaseSerializerFactory().getSerializerFor(TestResult.ResultType.class));

        @Override
        public TestCompleteEvent read(Decoder decoder) throws Exception {
            long endTime = decoder.readLong();
            TestResult.ResultType result = typeSerializer.read(decoder);
            return new TestCompleteEvent(endTime, result);
        }

        @Override
        public void write(Encoder encoder, TestCompleteEvent value) throws Exception {
            encoder.writeLong(value.getEndTime());
            typeSerializer.write(encoder, value.getResultType());
        }
    }

    private static class DefaultTestOutputEventSerializer implements Serializer<DefaultTestOutputEvent> {
        private final Serializer<TestOutputEvent.Destination> destinationSerializer = new BaseSerializerFactory().getSerializerFor(TestOutputEvent.Destination.class);

        @Override
        public DefaultTestOutputEvent read(Decoder decoder) throws Exception {
            TestOutputEvent.Destination destination = destinationSerializer.read(decoder);
            String message = decoder.readString();
            return new DefaultTestOutputEvent(destination, message);
        }

        @Override
        public void write(Encoder encoder, DefaultTestOutputEvent value) throws Exception {
            destinationSerializer.write(encoder, value.getDestination());
            encoder.writeString(value.getMessage());
        }
    }

    private static class DefaultTestFailureSerializer implements Serializer<DefaultTestFailure> {
        private final Serializer<Throwable> throwableSerializer;

        public DefaultTestFailureSerializer(Serializer<Throwable> throwableSerializer) {
            this.throwableSerializer = throwableSerializer;
        }

        @Override
        public DefaultTestFailure read(Decoder decoder) throws Exception {
            Throwable rawFailure = throwableSerializer.read(decoder);
            String message = decoder.readNullableString();
            String className = decoder.readNullableString();
            String stacktrace = decoder.readNullableString();
            boolean isAssertionFailure = decoder.readBoolean();
            String expected = decoder.readNullableString();
            String actual = decoder.readNullableString();
            return new DefaultTestFailure(rawFailure, new DefaultTestFailureDetails(message, className, stacktrace, isAssertionFailure, expected, actual));
        }

        @Override
        public void write(Encoder encoder, DefaultTestFailure value) throws Exception {
            throwableSerializer.write(encoder, value.getRawFailure());
            encoder.writeNullableString(value.getDetails().getMessage());
            encoder.writeNullableString(value.getDetails().getClassName());
            encoder.writeNullableString(value.getDetails().getStacktrace());
            encoder.writeBoolean(value.getDetails().isAssertionFailure());
            encoder.writeNullableString(value.getDetails().getExpected());
            encoder.writeNullableString(value.getDetails().getActual());
        }
    }

    private static class DefaultTestSuiteDescriptorSerializer implements Serializer<DefaultTestSuiteDescriptor> {
        final Serializer<CompositeIdGenerator.CompositeId> idSerializer = new IdSerializer();

        @Override
        public DefaultTestSuiteDescriptor read(Decoder decoder) throws Exception {
            Object id = idSerializer.read(decoder);
            String name = decoder.readString();
            return new DefaultTestSuiteDescriptor(id, name);
        }

        @Override
        public void write(Encoder encoder, DefaultTestSuiteDescriptor value) throws Exception {
            idSerializer.write(encoder, (CompositeIdGenerator.CompositeId) value.getId());
            encoder.writeString(value.getName());
        }
    }

    private static class DefaultNestedTestSuiteDescriptorSerializer implements Serializer<DefaultNestedTestSuiteDescriptor> {
        final Serializer<CompositeIdGenerator.CompositeId> idSerializer = new IdSerializer();

        @Override
        public DefaultNestedTestSuiteDescriptor read(Decoder decoder) throws Exception {
            Object id = idSerializer.read(decoder);
            String name = decoder.readString();
            String displayName = decoder.readString();
            CompositeIdGenerator.CompositeId parentId = idSerializer.read(decoder);
            return new DefaultNestedTestSuiteDescriptor(id, name, displayName, parentId);
        }

        @Override
        public void write(Encoder encoder, DefaultNestedTestSuiteDescriptor value) throws Exception {
            idSerializer.write(encoder, (CompositeIdGenerator.CompositeId) value.getId());
            encoder.writeString(value.getName());
            encoder.writeString(value.getDisplayName());
            idSerializer.write(encoder, value.getParentId());
        }
    }

    private static class WorkerTestSuiteDescriptorSerializer implements Serializer<WorkerTestClassProcessor.WorkerTestSuiteDescriptor> {
        final Serializer<CompositeIdGenerator.CompositeId> idSerializer = new IdSerializer();

        @Override
        public WorkerTestClassProcessor.WorkerTestSuiteDescriptor read(Decoder decoder) throws Exception {
            Object id = idSerializer.read(decoder);
            String name = decoder.readString();
            return new WorkerTestClassProcessor.WorkerTestSuiteDescriptor(id, name);
        }

        @Override
        public void write(Encoder encoder, WorkerTestClassProcessor.WorkerTestSuiteDescriptor value) throws Exception {
            idSerializer.write(encoder, (CompositeIdGenerator.CompositeId) value.getId());
            encoder.writeString(value.getName());
        }
    }

    private static class DefaultTestClassDescriptorSerializer implements Serializer<DefaultTestClassDescriptor> {
        final Serializer<CompositeIdGenerator.CompositeId> idSerializer = new IdSerializer();

        @Override
        public DefaultTestClassDescriptor read(Decoder decoder) throws Exception {
            Object id = idSerializer.read(decoder);
            String name = decoder.readString();
            String displayName = decoder.readString();
            return new DefaultTestClassDescriptor(id, name, displayName);
        }

        @Override
        public void write(Encoder encoder, DefaultTestClassDescriptor value) throws Exception {
            idSerializer.write(encoder, (CompositeIdGenerator.CompositeId) value.getId());
            encoder.writeString(value.getName());
            encoder.writeString(value.getDisplayName());
        }
    }

    private static class DefaultTestDescriptorSerializer implements Serializer<DefaultTestDescriptor> {
        final Serializer<CompositeIdGenerator.CompositeId> idSerializer = new IdSerializer();

        @Override
        public DefaultTestDescriptor read(Decoder decoder) throws Exception {
            Object id = idSerializer.read(decoder);
            String className = decoder.readString();
            String classDisplayName = decoder.readString();
            String name = decoder.readString();
            String displayName = decoder.readString();
            return new DefaultTestDescriptor(id, className, name, classDisplayName, displayName);
        }

        @Override
        public void write(Encoder encoder, DefaultTestDescriptor value) throws Exception {
            idSerializer.write(encoder, (CompositeIdGenerator.CompositeId) value.getId());
            encoder.writeString(value.getClassName());
            encoder.writeString(value.getClassDisplayName());
            encoder.writeString(value.getName());
            encoder.writeString(value.getDisplayName());
        }
    }

    private static class DefaultTestMethodDescriptorSerializer implements Serializer<DefaultTestMethodDescriptor> {
        final Serializer<CompositeIdGenerator.CompositeId> idSerializer = new IdSerializer();

        @Override
        public DefaultTestMethodDescriptor read(Decoder decoder) throws Exception {
            Object id = idSerializer.read(decoder);
            String className = decoder.readString();
            String name = decoder.readString();
            return new DefaultTestMethodDescriptor(id, className, name);
        }

        @Override
        public void write(Encoder encoder, DefaultTestMethodDescriptor value) throws Exception {
            idSerializer.write(encoder, (CompositeIdGenerator.CompositeId) value.getId());
            encoder.writeString(value.getClassName());
            encoder.writeString(value.getName());
        }
    }
}
