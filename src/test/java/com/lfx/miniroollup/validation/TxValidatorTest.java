package com.lfx.miniroollup.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.lfx.miniroollup.config.RollupPolicy;

import org.hyperledger.besu.datatypes.Transaction;

import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TxValidatorTest {

  private static final RollupPolicy POLICY = new RollupPolicy(true, 100, 1_000_000L, 50L);

  private TxValidator validator;

  @BeforeEach
  void setUp() {
    validator = new TxValidator();
  }

  @Test
  void validate_policyDisabled_alwaysAccepts() {
    final Transaction tx = txWith(999_999_999L, bytes(99_999));
    assertThat(validator.validate(tx, POLICY.withEnabled(false))).isEmpty();
    assertThat(validator.acceptedCount()).isEqualTo(1);
    assertThat(validator.rejectedCount()).isEqualTo(0);
  }

  @Test
  void validate_gasLimitAtBoundary_accepts() {
    assertThat(validator.validate(txWith(1_000_000L, Bytes.EMPTY), POLICY)).isEmpty();
    assertThat(validator.acceptedCount()).isEqualTo(1);
  }

  @Test
  void validate_gasLimitExceeded_rejects() {
    final var result = validator.validate(txWith(1_000_001L, Bytes.EMPTY), POLICY);
    assertThat(result).isPresent().get().asString().contains("gas limit exceeds policy");
    assertThat(validator.rejectedCount()).isEqualTo(1);
    assertThat(validator.acceptedCount()).isEqualTo(0);
  }

  @Test
  void validate_calldataAtBoundary_accepts() {
    assertThat(validator.validate(txWith(500_000L, bytes(100)), POLICY)).isEmpty();
    assertThat(validator.acceptedCount()).isEqualTo(1);
  }

  @Test
  void validate_calldataExceeded_rejects() {
    final var result = validator.validate(txWith(500_000L, bytes(101)), POLICY);
    assertThat(result).isPresent().get().asString().contains("calldata too large");
    assertThat(validator.rejectedCount()).isEqualTo(1);
  }

  @Test
  void validate_countersAccumulateAcrossMultipleCalls() {
    validator.validate(txWith(100L, Bytes.EMPTY), POLICY);
    validator.validate(txWith(100L, Bytes.EMPTY), POLICY);
    validator.validate(txWith(999_999_999L, Bytes.EMPTY), POLICY);
    assertThat(validator.acceptedCount()).isEqualTo(2);
    assertThat(validator.rejectedCount()).isEqualTo(1);
  }

  // ---- helpers ----

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
