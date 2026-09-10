package com.ailudick.capitalismmod.network;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CollectionSnapshotsTest {
    @Test
    void queuedHistorySurvivesMarketUpdatesDuringTraversal() {
        var candles = new ArrayList<>(List.of(100L, 120L));
        var history = new HashMap<>(Map.of("stock", candles));
        var snapshot = CollectionSnapshots.lists(history);
        var iterator = snapshot.get("stock").iterator();
        assertEquals(100L, iterator.next());
        // Market tick prunes and appends while the encoder is traversing the snapshot.
        candles.clear();
        candles.add(200L);
        history.clear();
        assertEquals(120L, iterator.next());
        assertFalse(iterator.hasNext());
        assertEquals(Map.of("stock", List.of(100L, 120L)), snapshot);
    }

    @Test
    void freezesEveryListAndOuterMap() {
        var snapshot = CollectionSnapshots.lists(Map.of(
                "a", new ArrayList<>(List.of(1L)), "b", new ArrayList<>(List.of(2L))));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.clear());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.get("a").clear());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.get("b").add(3L));
        assertEquals(Map.of(), CollectionSnapshots.lists(Map.of()));
    }
}
