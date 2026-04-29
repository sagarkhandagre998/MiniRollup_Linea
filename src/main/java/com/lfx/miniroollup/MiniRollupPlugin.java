package com.lfx.miniroollup;

import com.lfx.miniroollup.config.RollupPolicy;
import com.lfx.miniroollup.finalization.FinalizationTracker;
import com.lfx.miniroollup.metrics.MetricsCollector;
import com.lfx.miniroollup.rpc.RollupRpc;
import com.lfx.miniroollup.selection.TxSelectorFactory;
import com.lfx.miniroollup.validation.TxValidator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.hyperledger.besu.plugin.BesuPlugin;
import org.hyperledger.besu.plugin.ServiceManager;
import org.hyperledger.besu.plugin.services.BesuEvents;
import org.hyperledger.besu.plugin.services.MetricsSystem;
import org.hyperledger.besu.plugin.services.PicoCLIOptions;
import org.hyperledger.besu.plugin.services.RpcEndpointService;
import org.hyperledger.besu.plugin.services.TransactionSelectionService;
import org.hyperledger.besu.plugin.services.TransactionValidatorService;
import org.hyperledger.besu.plugin.services.metrics.MetricCategory;
import org.hyperledger.besu.plugin.services.metrics.MetricCategoryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Option;

public final class MiniRollupPlugin implements BesuPlugin {
  private static final Logger LOG = LoggerFactory.getLogger(MiniRollupPlugin.class);

  private ServiceManager context;
  private Long blockAddedListenerId;

  private final PluginOptions options = new PluginOptions();
  private final AtomicReference<RollupPolicy> policyRef = new AtomicReference<>(RollupPolicy.defaults());

  private final TxValidator txValidator = new TxValidator();
  private final TxSelectorFactory txSelectorFactory = new TxSelectorFactory(policyRef);
  private final FinalizationTracker finalizationTracker = new FinalizationTracker();
  private final MetricsCollector metricsCollector =
      new MetricsCollector(txValidator, txSelectorFactory, finalizationTracker);
  private final RollupRpc rollupRpc = new RollupRpc(policyRef, finalizationTracker);

  @Override
  public void register(final ServiceManager context) {
    this.context = context;
    context.getService(PicoCLIOptions.class).ifPresent(cli -> cli.addPicoCLIOptions("miniroollup", options));
    context
        .getService(MetricCategoryRegistry.class)
        .ifPresent(registry -> registry.addMetricCategory(MiniMetricCategory.MINIROLLUP));
    LOG.info("MiniRollupPlugin register() complete");
  }

  @Override
  public void beforeExternalServices() {
    final RollupPolicy policy =
        new RollupPolicy(
            options.enabled,
            options.maxCalldataBytes,
            options.maxGasLimit,
            options.maxBlockTxs);
    policyRef.set(policy);

    context.getService(TransactionValidatorService.class).ifPresent(this::registerValidator);
    context.getService(TransactionSelectionService.class).ifPresent(this::registerSelector);
    context.getService(RpcEndpointService.class).ifPresent(this::registerRpcEndpoints);

    LOG.info("MiniRollupPlugin beforeExternalServices() with policy {}", policy);
  }

  @Override
  public void start() {
    context.getService(BesuEvents.class).ifPresent(this::registerEventListeners);
    context.getService(MetricsSystem.class).ifPresent(this::registerMetricsBestEffort);
    LOG.info("MiniRollupPlugin start() complete");
  }

  @Override
  public void afterExternalServicePostMainLoop() {
    LOG.info("MiniRollupPlugin afterExternalServicePostMainLoop(): node is fully live");
  }

  @Override
  public CompletableFuture<Void> reloadConfiguration() {
    final RollupPolicy newPolicy =
        new RollupPolicy(
            options.enabled,
            options.maxCalldataBytes,
            options.maxGasLimit,
            options.maxBlockTxs);
    policyRef.set(newPolicy);
    LOG.info("MiniRollupPlugin configuration reloaded: {}", newPolicy);
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public void stop() {
    if (blockAddedListenerId != null) {
      context
          .getService(BesuEvents.class)
          .ifPresent(events -> events.removeBlockAddedListener(blockAddedListenerId));
      blockAddedListenerId = null;
    }
    LOG.info("MiniRollupPlugin stop() complete");
  }

  private void registerEventListeners(final BesuEvents events) {
    blockAddedListenerId =
        events.addBlockAddedListener(
            ctx -> finalizationTracker.onBlockObserved(ctx.getBlockHeader().getNumber()));
  }

  private void registerMetricsBestEffort(final MetricsSystem metricsSystem) {
    metricsCollector
        .gauges()
        .forEach(
            (name, supplier) -> {
              try {
                metricsSystem.createLongGauge(
                    MiniMetricCategory.MINIROLLUP,
                    "miniroollup_" + name,
                    "MiniRollupPlugin metric " + name,
                    supplier::get);
              } catch (final Exception e) {
                LOG.warn("Could not register metric {}: {}", name, e.getMessage());
              }
            });
  }

  private void registerValidator(final TransactionValidatorService service) {
    service.registerTransactionValidatorRule(tx -> txValidator.validate(tx, policyRef.get()));
    LOG.info("MiniRollupPlugin validator registered");
  }

  private void registerSelector(final TransactionSelectionService service) {
    service.registerPluginTransactionSelectorFactory(txSelectorFactory);
    LOG.info("MiniRollupPlugin selector factory registered");
  }

  private void registerRpcEndpoints(final RpcEndpointService rpc) {
    rpc.registerRPCEndpoint("miniroollup", "status", req -> rollupRpc.status());
    rpc.registerRPCEndpoint("miniroollup", "policy", req -> rollupRpc.policy());
    rpc.registerRPCEndpoint("miniroollup", "markHardFinalized", rollupRpc::markHardFinalized);
    LOG.info("MiniRollupPlugin RPC endpoints registered");
  }

  public static final class PluginOptions {
    @Option(names = "--plugin-miniroollup-enabled", defaultValue = "true")
    boolean enabled;

    @Option(names = "--plugin-miniroollup-max-calldata-bytes", defaultValue = "2048")
    int maxCalldataBytes;

    @Option(names = "--plugin-miniroollup-max-gas-limit", defaultValue = "10000000")
    long maxGasLimit;

    @Option(names = "--plugin-miniroollup-max-block-txs", defaultValue = "100")
    long maxBlockTxs;
  }

  private enum MiniMetricCategory implements MetricCategory {
    MINIROLLUP;

    @Override
    public String getName() {
      return "miniroollup";
    }

    @Override
    public java.util.Optional<String> getApplicationPrefix() {
      return java.util.Optional.of("plugin_");
    }
  }
}
