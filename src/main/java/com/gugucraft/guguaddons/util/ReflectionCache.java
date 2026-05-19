package com.gugucraft.guguaddons.util;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ReflectionCache {
    private ReflectionCache() {
    }

    public static MethodRef publicMethod(String className, String methodName, Class<?>... parameterTypes) {
        return new MethodRef(className, methodName, parameterTypes);
    }

    public static final class MethodRef {
        private final String className;
        private final String methodName;
        private final Class<?>[] parameterTypes;
        private volatile MethodLookup lookup;

        private MethodRef(String className, String methodName, Class<?>[] parameterTypes) {
            this.className = className;
            this.methodName = methodName;
            this.parameterTypes = parameterTypes.clone();
        }

        public MethodLookup lookup() {
            MethodLookup current = lookup;
            if (current != null) {
                return current;
            }

            synchronized (this) {
                current = lookup;
                if (current == null) {
                    current = resolve();
                    lookup = current;
                }
                return current;
            }
        }

        private MethodLookup resolve() {
            try {
                Class<?> targetClass = Class.forName(className);
                return MethodLookup.available(targetClass.getMethod(methodName, parameterTypes));
            } catch (ReflectiveOperationException | LinkageError | SecurityException exception) {
                return MethodLookup.unavailable(exception);
            }
        }
    }

    public static final class MethodLookup {
        private final Method method;
        private final Throwable failure;
        private final AtomicBoolean failureReported = new AtomicBoolean();

        private MethodLookup(Method method, Throwable failure) {
            this.method = method;
            this.failure = failure;
        }

        private static MethodLookup available(Method method) {
            return new MethodLookup(method, null);
        }

        private static MethodLookup unavailable(Throwable failure) {
            return new MethodLookup(null, failure);
        }

        public Method method() {
            return method;
        }

        public Throwable failure() {
            return failure;
        }

        public boolean reportFailure() {
            return method == null && failureReported.compareAndSet(false, true);
        }
    }
}
