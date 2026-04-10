package com.awesomeapp.app.java;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class Container5557 {

    public enum Lifecycle { TRANSIENT, SINGLETON, SCOPED }

    private static class Registration<T> {
        private final Class<T> type;
        private final Supplier<T> factory;
        private final Lifecycle lifecycle;
        private volatile T singletonInstance;

        Registration(Class<T> type, Supplier<T> factory, Lifecycle lifecycle) {
            this.type = type; this.factory = factory; this.lifecycle = lifecycle;
        }

        @SuppressWarnings("unchecked")
        T resolve(Map<Class<?>, Object> scopedInstances) {
            switch (lifecycle) {
                case SINGLETON:
                    if (singletonInstance == null) {
                        synchronized (this) {
                            if (singletonInstance == null) singletonInstance = factory.get();
                        }
                    }
                    return singletonInstance;
                case SCOPED:
                    return (T) scopedInstances.computeIfAbsent(type, k -> factory.get());
                default:
                    return factory.get();
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private final Map<Class<?>, Registration> registrations = new ConcurrentHashMap<>();
    private final Map<String, Map<Class<?>, Object>> scopes = new ConcurrentHashMap<>();
    private final Map<Class<?>, Class<?>> interfaceBindings = new HashMap<>();

    public <T> Container5557 registerSingleton(Class<T> type, Supplier<T> factory) {
        registrations.put(type, new Registration<>(type, factory, Lifecycle.SINGLETON));
        return this;
    }

    public <T> Container5557 registerTransient(Class<T> type, Supplier<T> factory) {
        registrations.put(type, new Registration<>(type, factory, Lifecycle.TRANSIENT));
        return this;
    }

    public <T> Container5557 registerScoped(Class<T> type, Supplier<T> factory) {
        registrations.put(type, new Registration<>(type, factory, Lifecycle.SCOPED));
        return this;
    }

    public <I, T extends I> Container5557 bind(Class<I> iface, Class<T> impl) {
        interfaceBindings.put(iface, impl);
        return this;
    }

    public <T> Container5557 registerInstance(Class<T> type, T instance) {
        registrations.put(type, new Registration<>(type, () -> instance, Lifecycle.SINGLETON));
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T resolve(Class<T> type) {
        return resolve(type, new HashMap<>());
    }

    @SuppressWarnings("unchecked")
    public <T> T resolve(Class<T> type, String scopeId) {
        Map<Class<?>, Object> scopedInstances = scopes.computeIfAbsent(scopeId, k -> new ConcurrentHashMap<>());
        return resolve(type, scopedInstances);
    }

    @SuppressWarnings("unchecked")
    private <T> T resolve(Class<T> type, Map<Class<?>, Object> scopedInstances) {
        Registration<T> reg = registrations.get(type);
        if (reg == null) {
            Class<?> impl = interfaceBindings.get(type);
            if (impl != null) reg = registrations.get(impl);
        }
        if (reg == null) throw new IllegalStateException("No registration found for: " + type.getSimpleName());
        return reg.resolve(scopedInstances);
    }

    public <T> Optional<T> tryResolve(Class<T> type) {
        try { return Optional.of(resolve(type)); } catch (Exception e) { return Optional.empty(); }
    }

    public void createScope(String scopeId) { scopes.put(scopeId, new ConcurrentHashMap<>()); }
    public void destroyScope(String scopeId) { scopes.remove(scopeId); }
    public boolean isRegistered(Class<?> type) { return registrations.containsKey(type) || interfaceBindings.containsKey(type); }
    public int getRegistrationCount() { return registrations.size(); }
}
