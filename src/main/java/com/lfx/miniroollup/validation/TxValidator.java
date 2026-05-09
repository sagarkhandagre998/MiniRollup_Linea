package com.lfx.miniroollup.validation;

import com.lfx.miniroollup.config.RollupPolicy;

import org.hyperledger.besu.datatypes.Transaction;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/** Validates transactions against the active rollup policy before pool admission. */
public final class TxValidator {

  private final AtomicLong accepted = new AtomicLong();
  private final AtomicLong rejected = new AtomicLong();

  public Optional<String> validate(final Transaction tx, final RollupPolicy policy) {
    if (!policy.enabled()) {
      accepted.incrementAndGet();
      return Optional.empty();
    }

    if (tx.getGasLimit() > policy.maxGasLimit()) {
      rejected.incrementAndGet();
      return Optional.of("Rejected by MiniRollupPlugin: gas limit exceeds policy");
    }

    final int payloadSize = tx.getPayload().size();
    if (payloadSize > policy.maxCalldataBytes()) {
      rejected.incrementAndGet();
      return Optional.of("Rejected by MiniRollupPlugin: calldata too large for proving budget");
    }

    accepted.incrementAndGet();
    return Optional.empty();
  }

  public long acceptedCount() {
    return accepted.get();
  }

  public long rejectedCount() {
    return rejected.get();
  }
}
