package com.lfx.miniroollup.options;

public static final class PluginOptions {

    @Option(names = "--plugin-miniroollup-enabled", defaultValue = "true")
    boolean enabled;

    @Option(
        names = "--plugin-miniroollup-max-calldata-bytes",
        defaultValue = "2048"
    )
    int maxCalldataBytes;

    @Option(
        names = "--plugin-miniroollup-max-gas-limit",
        defaultValue = "10000000"
    )
    long maxGasLimit;

    @Option(names = "--plugin-miniroollup-max-block-txs", defaultValue = "100")
    long maxBlockTxs;
}
