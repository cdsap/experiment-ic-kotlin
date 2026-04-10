package com.awesomeapp.cart.java;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class ConfigBuilder2024 {

    public enum LogLevel { TRACE, DEBUG, INFO, WARN, ERROR }
    public enum Environment { DEV, STAGING, PRODUCTION }

    public static final class Config {
        private final String name;
        private final Environment environment;
        private final LogLevel logLevel;
        private final int maxRetries;
        private final long timeoutMs;
        private final boolean enableMetrics;
        private final Map<String, String> properties;
        private final List<String> features;

        private Config(Builder builder) {
            this.name = builder.name;
            this.environment = builder.environment;
            this.logLevel = builder.logLevel;
            this.maxRetries = builder.maxRetries;
            this.timeoutMs = builder.timeoutMs;
            this.enableMetrics = builder.enableMetrics;
            this.properties = Collections.unmodifiableMap(new HashMap<>(builder.properties));
            this.features = Collections.unmodifiableList(new ArrayList<>(builder.features));
        }

        public String getName() { return name; }
        public Environment getEnvironment() { return environment; }
        public LogLevel getLogLevel() { return logLevel; }
        public int getMaxRetries() { return maxRetries; }
        public long getTimeoutMs() { return timeoutMs; }
        public boolean isMetricsEnabled() { return enableMetrics; }
        public Map<String, String> getProperties() { return properties; }
        public List<String> getFeatures() { return features; }
        public Optional<String> getProperty(String key) { return Optional.ofNullable(properties.get(key)); }
        public boolean hasFeature(String feature) { return features.contains(feature); }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Config)) return false;
            Config c = (Config) o;
            return maxRetries == c.maxRetries && timeoutMs == c.timeoutMs && enableMetrics == c.enableMetrics
                && Objects.equals(name, c.name) && environment == c.environment && logLevel == c.logLevel;
        }

        @Override
        public int hashCode() { return Objects.hash(name, environment, logLevel, maxRetries, timeoutMs, enableMetrics); }

        @Override
        public String toString() {
            return "Config{name='" + name + "', env=" + environment + ", log=" + logLevel +
                ", retries=" + maxRetries + ", timeout=" + timeoutMs + "ms, features=" + features.size() + "}";
        }
    }

    public static class Builder {
        private String name;
        private Environment environment = Environment.DEV;
        private LogLevel logLevel = LogLevel.INFO;
        private int maxRetries = 3;
        private long timeoutMs = 30000;
        private boolean enableMetrics = false;
        private final Map<String, String> properties = new HashMap<>();
        private final List<String> features = new ArrayList<>();

        public Builder(String name) { this.name = Objects.requireNonNull(name, "name must not be null"); }
        public Builder environment(Environment env) { this.environment = env; return this; }
        public Builder logLevel(LogLevel level) { this.logLevel = level; return this; }
        public Builder maxRetries(int retries) { this.maxRetries = retries; return this; }
        public Builder timeoutMs(long timeout) { this.timeoutMs = timeout; return this; }
        public Builder enableMetrics(boolean enable) { this.enableMetrics = enable; return this; }
        public Builder property(String key, String value) { properties.put(key, value); return this; }
        public Builder feature(String feature) { features.add(feature); return this; }

        public Config build() {
            validate();
            return new Config(this);
        }

        private void validate() {
            if (name.isEmpty()) throw new IllegalStateException("Name cannot be empty");
            if (maxRetries < 0) throw new IllegalStateException("Max retries must be >= 0");
            if (timeoutMs <= 0) throw new IllegalStateException("Timeout must be > 0");
            if (environment == Environment.PRODUCTION && logLevel == LogLevel.TRACE) {
                throw new IllegalStateException("TRACE logging not allowed in PRODUCTION");
            }
        }
    }

    public static Builder builder(String name) { return new Builder(name); }
}
