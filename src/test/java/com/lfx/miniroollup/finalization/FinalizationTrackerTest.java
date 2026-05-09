package com.lfx.miniroollup.finalization;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FinalizationTrackerTest {

  private FinalizationTracker tracker;

  @BeforeEach
  void setUp() {
    tracker = new FinalizationTracker();
  }

  @Test
  void initialState_isMinus1() {
    assertThat(tracker.latestSoftFinalizedBlock()).isEqualTo(-1L);
    assertThat(tracker.latestHardFinalizedBlock()).isEqualTo(-1L);
  }

  @Test
  void onBlockObserved_updatesSoftHeight() {
    tracker.onBlockObserved(10L);
    assertThat(tracker.latestSoftFinalizedBlock()).isEqualTo(10L);
  }

  @Test
  void onBlockObserved_isMonotonic_ignoresLowerBlock() {
    tracker.onBlockObserved(20L);
    tracker.onBlockObserved(15L);
    assertThat(tracker.latestSoftFinalizedBlock()).isEqualTo(20L);
  }

  @Test
  void onBlockObserved_doesNotAffectHardHeight() {
    tracker.onBlockObserved(10L);
    assertThat(tracker.latestHardFinalizedBlock()).isEqualTo(-1L);
  }

  @Test
  void markHardFinalized_updatesHardHeight() {
    tracker.markHardFinalized(5L);
    assertThat(tracker.latestHardFinalizedBlock()).isEqualTo(5L);
  }

  @Test
  void markHardFinalized_isMonotonic_ignoresLowerBlock() {
    tracker.markHardFinalized(100L);
    tracker.markHardFinalized(50L);
    assertThat(tracker.latestHardFinalizedBlock()).isEqualTo(100L);
  }

  @Test
  void softAndHard_trackIndependently() {
    tracker.onBlockObserved(42L);
    tracker.markHardFinalized(7L);
    assertThat(tracker.latestSoftFinalizedBlock()).isEqualTo(42L);
    assertThat(tracker.latestHardFinalizedBlock()).isEqualTo(7L);
  }
}
