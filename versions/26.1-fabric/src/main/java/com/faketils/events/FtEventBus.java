package com.faketils.events;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class FtEventBus {

    private static final Map<Class<? extends FtEvent>, List<Consumer<FtEvent>>> LISTENERS =
            new ConcurrentHashMap<>();

    private FtEventBus() {}

    @SuppressWarnings("unchecked")
    public static <T extends FtEvent> void onEvent(Class<T> type, Consumer<T> listener) {
        LISTENERS
                .computeIfAbsent(type, k -> new CopyOnWriteArrayList<>())
                .add((Consumer<FtEvent>) listener);
    }

    public static void emit(FtEvent event) {
        Class<? extends FtEvent> eventClass = event.getClass();

        List<Consumer<FtEvent>> exact = LISTENERS.get(eventClass);
        if (exact != null) {
            for (Consumer<FtEvent> c : exact) {
                c.accept(event);
            }
        }

        if (eventClass != FtEvent.class) {
            List<Consumer<FtEvent>> base = LISTENERS.get(FtEvent.class);
            if (base != null) {
                for (Consumer<FtEvent> c : base) {
                    c.accept(event);
                }
            }
        }
    }
}