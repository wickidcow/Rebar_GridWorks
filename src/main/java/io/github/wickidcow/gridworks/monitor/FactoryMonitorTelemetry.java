package io.github.wickidcow.gridworks.monitor;

import io.github.wickidcow.gridworks.api.control.ControlAddress;
import io.github.wickidcow.gridworks.api.control.ControlChannel;
import io.github.wickidcow.gridworks.api.control.ControlSignal;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe, source-qualified telemetry cache for Factory Monitor.
 *
 * <p>Built-in channels retain one latest value per source/channel pair.
 * Addressed commands retain only one latest command per source so changing
 * addresses cannot grow memory without bound. Sequence checks prevent an older
 * asynchronous callback from replacing newer state.</p>
 */
public final class FactoryMonitorTelemetry {
    private final ConcurrentHashMap<SourceChannel, ControlSignal> signals =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, ControlSignal> addressedSignals =
            new ConcurrentHashMap<>();

    public boolean observe(ControlSignal signal) {
        Objects.requireNonNull(signal, "signal");

        if (ControlAddress.isAddressedChannel(signal.channel())) {
            ControlSignal selected = addressedSignals.compute(
                    signal.source(),
                    (ignored, existing) -> newer(existing, signal)
            );
            return selected == signal;
        }

        SourceChannel key = new SourceChannel(signal.source(), signal.channel());
        ControlSignal selected = signals.compute(
                key,
                (ignored, existing) -> newer(existing, signal)
        );
        return selected == signal;
    }

    public Optional<ControlSignal> latest(UUID source, ControlChannel channel) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(channel, "channel");
        return Optional.ofNullable(signals.get(new SourceChannel(source, channel)));
    }

    public Optional<ControlSignal> latest(ControlChannel channel) {
        Objects.requireNonNull(channel, "channel");
        return signals.values().stream()
                .filter(signal -> channel.equals(signal.channel()))
                .max(Comparator.comparingLong(ControlSignal::sequence));
    }

    public Optional<ControlSignal> latestAddressed(UUID source) {
        Objects.requireNonNull(source, "source");
        return Optional.ofNullable(addressedSignals.get(source));
    }

    public Optional<ControlSignal> latestAddressed() {
        return addressedSignals.values().stream()
                .max(Comparator.comparingLong(ControlSignal::sequence));
    }

    public Set<UUID> sourceIds() {
        Set<UUID> sources = new HashSet<>(addressedSignals.keySet());
        for (SourceChannel key : signals.keySet()) {
            sources.add(key.source());
        }
        return Set.copyOf(sources);
    }

    public int signalCount() {
        return signals.size() + addressedSignals.size();
    }

    public int sourceSignalCount(UUID source) {
        Objects.requireNonNull(source, "source");

        int count = addressedSignals.containsKey(source) ? 1 : 0;
        for (SourceChannel key : signals.keySet()) {
            if (source.equals(key.source())) {
                count++;
            }
        }
        return count;
    }

    public void retainSources(Set<UUID> retainedSources) {
        Objects.requireNonNull(retainedSources, "retainedSources");
        signals.keySet().removeIf(key -> !retainedSources.contains(key.source()));
        addressedSignals.keySet().removeIf(source -> !retainedSources.contains(source));
    }

    private static ControlSignal newer(ControlSignal existing, ControlSignal candidate) {
        return existing == null || candidate.sequence() > existing.sequence()
                ? candidate
                : existing;
    }

    private record SourceChannel(UUID source, ControlChannel channel) {
        private SourceChannel {
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(channel, "channel");
        }
    }
}
