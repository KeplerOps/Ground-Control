package com.keplerops.groundcontrol.domain.testcases.service;

import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * TC-005 / ADR-043 — Shared sibling reorder algorithm.
 *
 * <p>Lives in the testcases service package because both
 * {@link TestCaseFolderService} (folder reorder) and
 * {@link TestCaseService} (test-case reorder) need the same semantics:
 * verify the requested list matches the container's current siblings
 * exactly, then renumber {@code sort_order} = 0..N-1 in the supplied
 * order. Keeping the algorithm here lets each aggregate's service own
 * its own placement writes while sharing the validation contract.
 */
final class SiblingOrderingHelper {

    private SiblingOrderingHelper() {}

    /**
     * Rejects partial / mismatched lists with {@link ConflictException}
     * and duplicates / nulls with {@link DomainValidationException}, then
     * calls {@code setOrder} on each sibling with its new 0..N-1
     * position. The caller is responsible for the in-project containment
     * guard on the parent container; this helper only sees siblings.
     */
    static <T> void applyOrdering(
            String entityLabel,
            List<UUID> orderedIds,
            List<T> currentSiblings,
            Function<T, UUID> idOf,
            BiConsumer<T, Integer> setOrder) {
        if (orderedIds == null) {
            throw new DomainValidationException(
                    "Ordered " + entityLabel + " id list is required", "invalid_reorder", Map.of());
        }
        if (orderedIds.size() != orderedIds.stream().distinct().count()) {
            throw new DomainValidationException(
                    "Ordered " + entityLabel + " id list contains duplicates", "invalid_reorder", Map.of());
        }
        var byId = new LinkedHashMap<UUID, T>();
        for (T item : currentSiblings) {
            byId.put(idOf.apply(item), item);
        }
        if (!byId.keySet().equals(new HashSet<>(orderedIds))) {
            throw new ConflictException("Reorder list must contain exactly the current siblings");
        }
        for (int index = 0; index < orderedIds.size(); index++) {
            T item = byId.get(orderedIds.get(index));
            setOrder.accept(item, index);
        }
    }
}
