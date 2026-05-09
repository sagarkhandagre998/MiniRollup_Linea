# MiniRollupPlugin

A Hyperledger Besu plugin that enforces L2 rollup sequencer policy on transaction pool admission and block transaction selection.

The plugin models the core gating logic a Linea-style sequencer applies before including transactions in a block. It attaches a `TransactionValidationRule` to the transaction pool and a `PluginTransactionSelectorFactory` to the block-building pipeline. Both enforce the same `RollupPolicy` — a gas-limit cap and a calldata-size cap — so transactions that would exceed the proving budget are rejected at the pool boundary and again at selection time. The active policy is hot-reloadable without restarting the node. Soft and hard finalization heights are tracked in memory and exposed over JSON-RPC and Prometheus gauges.

## Build

Requires Java 21 or later.

```
./gradlew build
```

The plugin jar is produced at `build/libs/MiniRollupPlugin-0.1.0.jar`. Place it in Besu's `plugins/` directory before starting the node.

## Install

```
cp build/libs/MiniRollupPlugin-0.1.0.jar $BESU_HOME/plugins/
```

Then start Besu normally. The plugin is auto-discovered via the `META-INF/services/org.hyperledger.besu.plugin.BesuPlugin` service file.

Enable metrics scraping with `--metrics-category=MINIROLLUP` if you want the gauges exposed on the Prometheus endpoint.

## CLI flags

All flags are registered under the `--plugin-miniroollup-*` prefix.

| Flag | Default | Purpose |
|---|---|---|
| `--plugin-miniroollup-enabled` | `true` | Master toggle. When `false`, both the validator and the selector become no-ops and all transactions pass through. |
| `--plugin-miniroollup-max-calldata-bytes` | `2048` | Maximum calldata payload size in bytes. Transactions with a larger payload are rejected. |
| `--plugin-miniroollup-max-gas-limit` | `10000000` | Maximum gas limit per transaction. Transactions declaring a higher gas limit are rejected. |
| `--plugin-miniroollup-max-block-txs` | `100` | Soft cap on the number of transactions per block. Currently tracked in the finalization model; selector enforcement uses gas and calldata limits. |

## JSON-RPC methods

The plugin contributes three methods under the `miniroollup_` namespace.

### `miniroollup_status`

Returns the current plugin state: whether it is enabled, and the latest soft and hard finalized block heights.

Parameters: none.

Returns: a status object.

```
curl -X POST -H "Content-Type: application/json" \
  --data '{"jsonrpc":"2.0","method":"miniroollup_status","params":[],"id":1}' \
  http://localhost:8545
```

Response shape:

```json
{
  "plugin": "MiniRollupPlugin",
  "enabled": true,
  "softFinalizedBlock": 104200,
  "hardFinalizedBlock": 103950
}
```

`softFinalizedBlock` is the highest block number observed by the `BesuEvents` block-added listener. It advances automatically as the node imports blocks. `hardFinalizedBlock` is set explicitly via `miniroollup_markHardFinalized` and represents an externally confirmed finalization point. Both start at `-1` before the first block is seen or marked.

### `miniroollup_policy`

Returns the active rollup policy values. These reflect the values parsed from CLI flags at startup and re-read on each hot-reload.

Parameters: none.

Returns: a policy object.

```
curl -X POST -H "Content-Type: application/json" \
  --data '{"jsonrpc":"2.0","method":"miniroollup_policy","params":[],"id":1}' \
  http://localhost:8545
```

Response shape:

```json
{
  "enabled": true,
  "maxCalldataBytes": 2048,
  "maxGasLimit": 10000000,
  "maxBlockTxs": 100
}
```

### `miniroollup_markHardFinalized`

Advances the hard finalization height. The update is monotonic — passing a block number lower than the current hard-finalized height is a no-op.

Parameters: a single integer block number.

Returns: a confirmation object with the new hard-finalized height.

```
curl -X POST -H "Content-Type: application/json" \
  --data '{"jsonrpc":"2.0","method":"miniroollup_markHardFinalized","params":["103950"],"id":1}' \
  http://localhost:8545
```

Response shape:

```json
{
  "ok": true,
  "newHardFinalizedBlock": 103950
}
```

## Prometheus metrics

Registered under the metric category `MINIROLLUP` with application prefix `plugin_`. Enable with `--metrics-category=MINIROLLUP`. All metrics are gauges; Besu renders the Prometheus name as `plugin_miniroollup_<shortName>`.

| Metric | Rendered name | Description |
|---|---|---|
| `accepted_tx_total` | `plugin_miniroollup_accepted_tx_total` | Cumulative transactions accepted by the validator. |
| `rejected_tx_total` | `plugin_miniroollup_rejected_tx_total` | Cumulative transactions rejected by the validator. |
| `selected_tx_total` | `plugin_miniroollup_selected_tx_total` | Cumulative transactions selected by the block selector. |
| `dropped_tx_total` | `plugin_miniroollup_dropped_tx_total` | Cumulative transactions dropped by the block selector. |
| `soft_finalized_block` | `plugin_miniroollup_soft_finalized_block` | Latest block number observed via `BesuEvents`. `-1` before the first block. |
| `hard_finalized_block` | `plugin_miniroollup_hard_finalized_block` | Latest block number marked hard-finalized via RPC. `-1` until first call. |

## Hot-reload

The plugin honours Besu's `BesuPlugin.reloadConfiguration` lifecycle hook. When triggered, it constructs a new `RollupPolicy` from the current values of the CLI option fields and atomically swaps it on the `AtomicReference` shared by the validator and the selector. In-flight transactions observe the new policy at the very next validation or selection call after the swap completes.

## Package structure

```
src/main/java/com/lfx/miniroollup/
├── MiniRollupPlugin.java          # BesuPlugin lifecycle — register, beforeExternalServices, start, stop
├── config/
│   └── RollupPolicy.java          # Immutable value object holding all policy limits
├── finalization/
│   └── FinalizationTracker.java   # Thread-safe soft and hard finalization height tracking
├── metrics/
│   ├── MetricsCollector.java      # Assembles gauge name-to-supplier map for MetricsSystem registration
│   └── MiniMetricCategory.java    # MetricCategory enum — category name "miniroollup", prefix "plugin_"
├── options/
│   └── PluginOptions.java         # PicoCLI option holder registered via PicoCLIOptions
├── rpc/
│   └── RollupRpc.java             # Handler methods for the three miniroollup_* JSON-RPC endpoints
├── selection/
│   └── TxSelectorFactory.java     # PluginTransactionSelectorFactory — enforces gas and calldata limits at selection
└── validation/
    └── TxValidator.java           # TransactionValidationRule — enforces gas and calldata limits at pool admission
```

## Plugin API services consumed

Five services from `org.hyperledger.besu.plugin.services`:

- `PicoCLIOptions`: used during `register()` to bind the four `--plugin-miniroollup-*` CLI flags.
- `MetricCategoryRegistry`: used during `register()` to register the `MINIROLLUP` category.
- `TransactionValidatorService`: used during `beforeExternalServices()` to register the `TransactionValidationRule` that gates pool admission.
- `TransactionSelectionService`: used during `beforeExternalServices()` to register the `PluginTransactionSelectorFactory` that gates block inclusion.
- `RpcEndpointService`: used during `beforeExternalServices()` to wire the three `miniroollup_*` methods.
- `BesuEvents`: used during `start()` to subscribe the block-added listener that advances the soft finalization height.
- `MetricsSystem`: used during `start()` to register the six long gauges.

## Out of scope

The plugin is deliberately scoped to in-memory policy enforcement and finalization tracking. Out of scope:

- Persistent storage of rejected or dropped transactions.
- ABI decoding of transaction calldata.
- Per-sender or per-contract policy differentiation.
- Cross-plugin communication or shared state.
- Webhook or alerting integrations.

## Test

```
./gradlew test
```

Three test suites covering the core domain logic:

| Suite | Tests | What is covered |
|---|---|---|
| `FinalizationTrackerTest` | 7 | Initial state is `-1`, soft height advances on block observed, soft height is monotonic, soft does not affect hard, hard height advances on mark, hard height is monotonic, soft and hard are independent |
| `TxValidatorTest` | 6 | Policy disabled bypasses all checks, gas at boundary accepts, gas exceeded rejects with correct message, calldata at boundary accepts, calldata exceeded rejects with correct message, accepted and rejected counters accumulate correctly |
| `TxSelectorFactoryTest` | 7 | Policy disabled selects everything, gas at boundary selects, gas exceeded drops, calldata at boundary selects, calldata exceeded drops, post-processing always selects, selected and dropped counters accumulate correctly |
