package com.lfx.miniroollup.metrics;

import org.hyperledger.besu.plugin.services.metrics.MetricCategory;

import java.util.Optional;

/** Metric category for all MiniRollupPlugin gauges, registered under the "plugin_" prefix. */
public enum MiniMetricCategory implements MetricCategory {
  MINIROLLUP;

  @Override
  public String getName() {
    return "miniroollup";
  }

  @Override
  public Optional<String> getApplicationPrefix() {
    return Optional.of("plugin_");
  }
}
