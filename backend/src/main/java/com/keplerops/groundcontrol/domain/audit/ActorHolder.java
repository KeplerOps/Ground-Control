package com.keplerops.groundcontrol.domain.audit;

/**
 * Thread-local holder for the current actor identity.
 * Set before a mutation, cleared after commit.
 */
public final class ActorHolder {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private ActorHolder() {}

    public static void set(String actor) {
        CURRENT.set(actor);
    }

    public static String get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
