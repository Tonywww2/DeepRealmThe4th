package com.tonywww.deeprealm4th.worldgen.hydrology;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

/** Bounded LRU bookkeeping with expensive, deterministic calculations outside the monitor. */
final class SingleFlightCache<V> {
    private static final ThreadLocal<Object> OWNER=ThreadLocal.withInitial(Object::new);
    private final int limit;
    private final Map<Long, Object> values;
    private long epoch;

    private static final class InFlight<V> {
        final CompletableFuture<V> result=new CompletableFuture<>();
        final Object owner=OWNER.get();
    }

    SingleFlightCache(int limit) {
        this.limit = limit;
        values = new LinkedHashMap<>(limit, .75f, true);
    }

    V get(long key, Supplier<V> calculation) {
        InFlight<V> pending;
        boolean creator=false;
        synchronized (this) {
            Object cached=values.get(key);
            if(cached==null) {
                pending=new InFlight<>();values.put(key,pending);creator=true;
            } else if(cached instanceof InFlight<?> flight) {
                @SuppressWarnings("unchecked") InFlight<V> existing=(InFlight<V>)flight;
                pending=existing;
            } else {
                @SuppressWarnings("unchecked") V value=(V)cached;
                return value;
            }
            if(!creator&&pending.owner==OWNER.get()) {
                throw new IllegalStateException("Recursive worldgen cache request for " + key);
            }
        }
        if (creator) {
            try {
                V value = Objects.requireNonNull(calculation.get(),"Worldgen cache values must not be null");
                synchronized (this) {
                    if(values.get(key)==pending)values.put(key,value);
                    trim();
                }
                pending.result.complete(value);
                return value;
            } catch (Throwable error) {
                pending.result.completeExceptionally(error);
                synchronized (this) { if (values.get(key) == pending) values.remove(key); }
                throw rethrow(error);
            }
        }
        try {
            return pending.result.join();
        } catch (CompletionException error) {
            throw rethrow(error.getCause());
        }
    }

    /** Completed entries only: never wait for another traversal while holding a graph path. */
    @SuppressWarnings("unchecked")
    synchronized V completed(long key) {
        Object entry=values.get(key);
        return entry==null||entry instanceof InFlight<?>?null:(V)entry;
    }

    synchronized long epoch() { return epoch; }

    synchronized void remember(long key, V value, long expectedEpoch) {
        if (epoch != expectedEpoch || values.containsKey(key)) return;
        values.put(key,value);
        trim();
    }

    synchronized void clear() { values.clear(); epoch++; }

    private void trim() {
        if (values.size() <= limit) return;
        Iterator<Object> iterator = values.values().iterator();
        while (values.size() > limit && iterator.hasNext()) {
            if (!(iterator.next() instanceof InFlight<?>)) iterator.remove();
        }
        // At most the concurrently running calculations may temporarily exceed the bound.
    }

    private static RuntimeException rethrow(Throwable error) {
        if (error instanceof Error fatal) throw fatal;
        if (error instanceof RuntimeException runtime) return runtime;
        return new IllegalStateException("Worldgen cache calculation failed", error);
    }
}
