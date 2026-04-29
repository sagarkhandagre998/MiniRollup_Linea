# MiniRollupPlugin

A minimal Linea-inspired Besu plugin built for LFX evaluation.

## Goal

Demonstrate:

- Practical understanding of Besu plugin lifecycle hooks
- Use of core Plugin API extension points
- Awareness of L2/rollup trade-offs (validation vs throughput, soft vs hard finality)

This is intentionally not a full zk-rollup implementation.

## Project structure

```text
MiniRollupPlugin
 ├── config/
 │     └── RollupPolicy.java
 │
 ├── validation/
 │     └── TxValidator.java
 │
 ├── selection/
 │     └── TxSelectorFactory.java
 │
 ├── finalization/
 │     └── FinalizationTracker.java
 │
 ├── metrics/
 │     └── MetricsCollector.java
 │
 ├── rpc/
 │     └── RollupRpc.java
 │
 └── MiniRollupPlugin.java
```

## Lifecycle mapping

- `register()`
  - Save `ServiceManager`
  - Register plugin CLI options through `PicoCLIOptions`
- `beforeExternalServices()`
  - Materialize policy from CLI flags
  - Prepare validator/selector/RPC registration points
- `start()`
  - Subscribe to block events using `BesuEvents`
  - Register gauges using `MetricsSystem`
- `afterExternalServicePostMainLoop()`
  - Mark plugin as fully live
- `reloadConfiguration()`
  - Re-read active policy from parsed options
- `stop()`
  - Unsubscribe listeners and clean up

## Linea-inspired behavior (minimal)

- **Policy-based tx admission** in `TxValidator` (gas limit + calldata budget)
- **Sequencer-like tx selection policy** in `TxSelectorFactory`
- **Soft/hard finality state tracking** in `FinalizationTracker`
- **RPC payload handlers** for status/policy/finality in `RollupRpc`
- **Operational metrics** in `MetricsCollector`

## Build

```bash
./gradlew clean jar
```

The JAR can then be placed in Besu's `plugins/` directory.

## Notes

- Some service API signatures vary slightly across Besu releases.
- This sample keeps wiring lightweight and interview-friendly; complete production wiring can be pinned to a chosen Besu version in a follow-up step.
