package com.lfx.miniroollup.options;

import picocli.CommandLine.Option;

/** CLI options injected by Besu via PicoCLIOptions during plugin registration. */
public final class PluginOptions {

  @Option(names = "--plugin-miniroollup-enabled", defaultValue = "true")
  public boolean enabled;

  @Option(names = "--plugin-miniroollup-max-calldata-bytes", defaultValue = "2048")
  public int maxCalldataBytes;

  @Option(names = "--plugin-miniroollup-max-gas-limit", defaultValue = "10000000")
  public long maxGasLimit;

  @Option(names = "--plugin-miniroollup-max-block-txs", defaultValue = "100")
  public long maxBlockTxs;
}
