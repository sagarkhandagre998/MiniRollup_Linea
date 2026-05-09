package com.lfx.miniroollup.selection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.lfx.miniroollup.config.RollupPolicy;

import org.hyperledger.besu.datatypes.PendingTransaction;
import org.hyperledger.besu.datatypes.Transaction;
import org.hyperledger.besu.plugin.data.ProcessableBlockHeader;
import org.hyperledger.besu.plugin.data.TransactionProcessingResult;
import org.hyperledger.besu.plugin.data.TransactionSelectionResult;
import org.hyperledger.besu.plugin.services.txselection.PluginTransactionSelector;
import org.hyperledger.besu.plugin.services.txselection.SelectorsStateManager;
import org.hyperledger.besu.plugin.services.txselection.TransactionEvaluationContext;

import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

class TxSelectorFactoryTest {

  private static final RollupPolicy POLICY = new RollupPolicy(true, 100, 1_000_000L, 50L);

  private AtomicReference<RollupPolicy> policyRef;
  private TxSelectorFactory factory;

  @BeforeEach
  void setUp() {
    policyRef = new AtomicReference<>(POLICY);
    factory = new TxSelectorFactory(policyRef);
  }

  @Test
  void select_policyDisabled_alwaysSelects() {
    policyRef.set(POLICY.withEnabled(false));
    assertThat(evalPreProcessing(txWith(999_999_999L, bytes(99_999))).selected()).isTrue();
    assertThat(factory.selectedCount()).isEqualTo(1);
    assertThat(factory.droppedCount()).isEqualTo(0);
  }

  @Test
  void select_gasLimitAtBoundary_selects() {
    assertThat(evalPreProcessing(txWith(1_000_000L, Bytes.EMPTY)).selected()).isTrue();
  }

  @Test
  void select_gasLimitExceeded_drops() {
    assertThat(evalPreProcessing(txWith(1_000_001L, Bytes.EMPTY)).selected()).isFalse();
    assertThat(factory.droppedCount()).isEqualTo(1);
    assertThat(factory.selectedCount()).isEqualTo(0);
  }

  @Test
  void select_calldataAtBoundary_selects() {
    assertThat(evalPreProcessing(txWith(500_000L, bytes(100))).selected()).isTrue();
  }

  @Test
  void select_calldataExceeded_drops() {
    assertThat(evalPreProcessing(txWith(500_000L, bytes(101))).selected()).isFalse();
    assertThat(factory.droppedCount()).isEqualTo(1);
  }

  @Test
  void postProcessing_alwaysSelects() {
    final PluginTransactionSelector selector = newSelector();
    final TransactionSelectionResult result =
        selector.evaluateTransactionPostProcessing(
            mock(TransactionEvaluationContext.class), mock(TransactionProcessingResult.class));
    assertThat(result.selected()).isTrue();
  }

  @Test
  void counters_accumulateAcrossMultipleSelections() {
    evalPreProcessing(txWith(100L, Bytes.EMPTY));
    evalPreProcessing(txWith(100L, Bytes.EMPTY));
    evalPreProcessing(txWith(999_999_999L, Bytes.EMPTY));
    assertThat(factory.selectedCount()).isEqualTo(2);
    assertThat(factory.droppedCount()).isEqualTo(1);
  }

  // ---- helpers ----

  private TransactionSelectionResult evalPreProcessing(final Transaction tx) {
    return newSelector().evaluateTransactionPreProcessing(ctxFor(tx));
  }

  private PluginTransactionSelector newSelector() {
    return factory.create(mock(ProcessableBlockHeader.class), mock(SelectorsStateManager.class));
  }

  private static TransactionEvaluationContext ctxFor(final Transaction tx) {
    final PendingTransaction pending = mock(PendingTransaction.class);
    when(pending.getTransaction()).thenReturn(tx);
    final TransactionEvaluationContext ctx = mock(TransactionEvaluationContext.class);
    when(ctx.getPendingTransaction()).thenReturn(pending);
    return ctx;
  }

  private static Transaction txWith(final long gasLimit, final Bytes payload) {
    final Transaction tx = mock(Transaction.class);
    when(tx.getGasLimit()).thenReturn(gasLimit);
    when(tx.getPayload()).thenReturn(payload);
    return tx;
  }

  private static Bytes bytes(final int size) {
    return Bytes.wrap(new byte[size]);
  }
}
