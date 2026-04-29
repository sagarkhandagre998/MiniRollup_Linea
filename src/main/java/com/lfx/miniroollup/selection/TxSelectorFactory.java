package com.lfx.miniroollup.selection;

import static org.hyperledger.besu.plugin.data.TransactionSelectionResult.SELECTED;

import com.lfx.miniroollup.config.RollupPolicy;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.hyperledger.besu.plugin.data.TransactionProcessingResult;
import org.hyperledger.besu.plugin.data.TransactionSelectionResult;
import org.hyperledger.besu.plugin.services.txselection.PluginTransactionSelector;
import org.hyperledger.besu.plugin.services.txselection.PluginTransactionSelectorFactory;
import org.hyperledger.besu.plugin.services.txselection.SelectorsStateManager;
import org.hyperledger.besu.plugin.services.txselection.TransactionEvaluationContext;

/**
 * Minimal "sequencer policy" helper that can be mapped to TransactionSelectionService hooks.
 */
public final class TxSelectorFactory implements PluginTransactionSelectorFactory {
  private final AtomicReference<RollupPolicy> policyRef;
  private final AtomicLong selected = new AtomicLong();
  private final AtomicLong dropped = new AtomicLong();

  public TxSelectorFactory(final AtomicReference<RollupPolicy> policyRef) {
    this.policyRef = policyRef;
  }

  @Override
  public PluginTransactionSelector create(final SelectorsStateManager selectorsStateManager) {
    return new PluginTransactionSelector() {
      @Override
      public TransactionSelectionResult evaluateTransactionPreProcessing(
          final TransactionEvaluationContext evaluationContext) {
        final RollupPolicy policy = policyRef.get();
        if (!policy.enabled()) {
          selected.incrementAndGet();
          return SELECTED;
        }

        final var tx = evaluationContext.getPendingTransaction().getTransaction();
        if (tx.getGasLimit() > policy.maxGasLimit()) {
          dropped.incrementAndGet();
          return TransactionSelectionResult.invalid("gas limit exceeds mini rollup policy");
        }
        if (tx.getPayload().size() > policy.maxCalldataBytes()) {
          dropped.incrementAndGet();
          return TransactionSelectionResult.invalid("calldata too large for mini rollup policy");
        }

        selected.incrementAndGet();
        return SELECTED;
      }

      @Override
      public TransactionSelectionResult evaluateTransactionPostProcessing(
          final TransactionEvaluationContext evaluationContext,
          final TransactionProcessingResult processingResult) {
        return SELECTED;
      }
    };
  }

  @Deprecated(forRemoval = true)
  public boolean shouldSelectLegacy(final long txCountInBlock) {
    final RollupPolicy policy = policyRef.get();
    if (!policy.enabled()) {
      selected.incrementAndGet();
      return true;
    }

    if (txCountInBlock >= policy.maxBlockTxs()) {
      dropped.incrementAndGet();
      return false;
    }

    selected.incrementAndGet();
    return true;
  }

  public long selectedCount() {
    return selected.get();
  }

  public long droppedCount() {
    return dropped.get();
  }
}
