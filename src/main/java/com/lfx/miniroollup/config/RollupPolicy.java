package com.lfx.miniroollup.config;

import java.util.Objects;

/** Immutable rollup policy applied by both transaction validation and block selection. */
public final class RollupPolicy {

  private final boolean enabled;
  private final int maxCalldataBytes;
  private final long maxGasLimit;
  private final long maxBlockTxs;

  public RollupPolicy(
      final boolean enabled,
      final int maxCalldataBytes,
      final long maxGasLimit,
      final long maxBlockTxs) {
    this.enabled = enabled;
    this.maxCalldataBytes = maxCalldataBytes;
    this.maxGasLimit = maxGasLimit;
    this.maxBlockTxs = maxBlockTxs;
  }

  public static RollupPolicy defaults() {
    return new RollupPolicy(true, 2048, 10_000_000L, 100L);
  }

  public boolean enabled() {
    return enabled;
  }

  public int maxCalldataBytes() {
    return maxCalldataBytes;
  }

  public long maxGasLimit() {
    return maxGasLimit;
  }

  public long maxBlockTxs() {
    return maxBlockTxs;
  }

  public RollupPolicy withEnabled(final boolean newEnabled) {
    return new RollupPolicy(newEnabled, maxCalldataBytes, maxGasLimit, maxBlockTxs);
  }

  @Override
  public String toString() {
    return ("RollupPolicy{"
        + "enabled="
        + enabled
        + ", maxCalldataBytes="
        + maxCalldataBytes
        + ", maxGasLimit="
        + maxGasLimit
        + ", maxBlockTxs="
        + maxBlockTxs
        + '}');
  }

  @Override
  public int hashCode() {
    return Objects.hash(enabled, maxCalldataBytes, maxGasLimit, maxBlockTxs);
  }

  @Override
  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof RollupPolicy that)) {
      return false;
    }
    return (enabled == that.enabled
        && maxCalldataBytes == that.maxCalldataBytes
        && maxGasLimit == that.maxGasLimit
        && maxBlockTxs == that.maxBlockTxs);
  }
}
