package com.lfx.miniroollup.metrics;

import com.lfx.miniroollup.finalization.FinalizationTracker;
import com.lfx.miniroollup.selection.TxSelectorFactory;
import com.lfx.miniroollup.validation.TxValidator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Keeps metrics collection independent from Besu-specific metric registration plumbing.
 */
public final class MetricsCollector {
  private final TxValidator validator;
  private final TxSelectorFactory selector;
  private final FinalizationTracker finalizationTracker;

  public MetricsCollector(
      final TxValidator validator,
      final TxSelectorFactory selector,
      final FinalizationTracker finalizationTracker) {
    this.validator = validator;
    this.selector = selector;
    this.finalizationTracker = finalizationTracker;
  }

  public Map<String, Supplier<Long>> gauges() {
    final Map<String, Supplier<Long>> map = new LinkedHashMap<>();
    map.put("accepted_tx_total", validator::acceptedCount);
    map.put("rejected_tx_total", validator::rejectedCount);
    map.put("selected_tx_total", selector::selectedCount);
    map.put("dropped_tx_total", selector::droppedCount);
    map.put("soft_finalized_block", finalizationTracker::latestSoftFinalizedBlock);
    map.put("hard_finalized_block", finalizationTracker::latestHardFinalizedBlock);
    return map;
  }
}
