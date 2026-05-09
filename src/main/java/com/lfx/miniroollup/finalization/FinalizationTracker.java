package com.lfx.miniroollup.finalization;

import java.util.concurrent.atomic.AtomicLong;

/** Tracks soft (latest observed block) and hard (externally confirmed) finalization heights. */
public final class FinalizationTracker {

  private final AtomicLong latestSoftFinalizedBlock = new AtomicLong(-1L);
  private final AtomicLong latestHardFinalizedBlock = new AtomicLong(-1L);

  public void onBlockObserved(final long blockNumber) {
    latestSoftFinalizedBlock.accumulateAndGet(blockNumber, Math::max);
  }

  public void markHardFinalized(final long blockNumber) {
    latestHardFinalizedBlock.accumulateAndGet(blockNumber, Math::max);
  }

  public long latestSoftFinalizedBlock() {
    return latestSoftFinalizedBlock.get();
  }

  public long latestHardFinalizedBlock() {
    return latestHardFinalizedBlock.get();
  }
}
