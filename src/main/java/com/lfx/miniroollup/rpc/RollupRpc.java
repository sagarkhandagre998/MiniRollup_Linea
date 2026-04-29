package com.lfx.miniroollup.rpc;

import com.lfx.miniroollup.config.RollupPolicy;
import com.lfx.miniroollup.finalization.FinalizationTracker;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.hyperledger.besu.plugin.services.rpc.PluginRpcRequest;

/**
 * RPC payload handlers used by MiniRollupPlugin.
 * Registration to Besu RpcEndpointService is handled in the plugin lifecycle class.
 */
public final class RollupRpc {
  private final AtomicReference<RollupPolicy> policyRef;
  private final FinalizationTracker finalizationTracker;

  public RollupRpc(
      final AtomicReference<RollupPolicy> policyRef, final FinalizationTracker finalizationTracker) {
    this.policyRef = policyRef;
    this.finalizationTracker = finalizationTracker;
  }

  public Map<String, Object> status() {
    final Map<String, Object> out = new LinkedHashMap<>();
    out.put("plugin", "MiniRollupPlugin");
    out.put("enabled", policyRef.get().enabled());
    out.put("softFinalizedBlock", finalizationTracker.latestSoftFinalizedBlock());
    out.put("hardFinalizedBlock", finalizationTracker.latestHardFinalizedBlock());
    return out;
  }

  public Map<String, Object> policy() {
    final RollupPolicy p = policyRef.get();
    final Map<String, Object> out = new LinkedHashMap<>();
    out.put("enabled", p.enabled());
    out.put("maxCalldataBytes", p.maxCalldataBytes());
    out.put("maxGasLimit", p.maxGasLimit());
    out.put("maxBlockTxs", p.maxBlockTxs());
    return out;
  }

  public Map<String, Object> markHardFinalized(final PluginRpcRequest request) {
    final Object[] params = request.getParams();
    if (params.length == 0) {
      throw new IllegalArgumentException("Expected block number parameter");
    }
    final long blockNumber = Long.parseLong(params[0].toString());
    finalizationTracker.markHardFinalized(blockNumber);
    final Map<String, Object> out = new LinkedHashMap<>();
    out.put("ok", true);
    out.put("newHardFinalizedBlock", finalizationTracker.latestHardFinalizedBlock());
    return out;
  }
}
