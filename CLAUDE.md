# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with
code in this repository.

## Commands

```bash
# Validate workspace (dependency graph, naming, etc.)
clj -M:poly check

# Run all tests
clj -M:poly test :dev

# Show component overview
clj -M:poly info

# Run tests for a specific component (from the project root)
clj -M:poly test :dev :component assert
```

## Architecture

This is a [Polylith](https://polylith.gitbook.io) monorepo. The key structural
rule: **components only expose their `interface` namespace to consumers; `core`
is private implementation**.

- components/
  - assert/ – assertion macros (always, sometimes, reachable, unreachable) +
    numeric/boolean guidance
  - handler/ – output handler selection (FFI → local file → no-op); exposes
    output-json! and rand-long
  - lifecycle/ – setup-complete! and send-event!
  - random/ – get-random and random-choice
- projects/
  - antithesis-sdk/ – deployable artifact; wires the four components together

**Handler selection** is done once lazily via `defonce` in `handler/core.clj`.
Priority: JNI `FfiHandler` (inside Antithesis) → `ANTITHESIS_SDK_LOCAL_OUTPUT`
env var (writes JSONL) → no-op. The handler is never allowed to throw — graceful
degradation outside Antithesis is a hard requirement.

**Assertion deduplication** is the central behavioural invariant:
`assert/core.clj` keeps a per-message `{:pass-count :fail-count}` atom.
Output is emitted exactly once when pass-count first reaches 1, and once when
fail-count first reaches 1. Catalog registrations (`hit=false`) bypass the
tracker and always emit.

**Numeric guidance deduplication**: a separate `guidance-tracker` atom stores
the best gap per message. Emission only occurs when the new gap strictly
improves on the stored mark (or is NaN — NaN always emits but never updates
the mark).

**The Allium spec** (`antithesis-sdk.allium`) is the authoritative behavioural
contract. Tests in `components/assert/test/` are explicitly mapped to spec rules
(comments like `--- rule-success.AlwaysAssert ---`). When adding or changing
behaviour, update the spec and keep the test–rule annotations in sync.

## Key dependencies

| Dep                  | Role                                                       |
| -------------------- | ---------------------------------------------------------- |
| `com.antithesis/ffi` | JNI bridge to the Antithesis platform                      |
| `metosin/jsonista`   | JSON serialisation                                         |
| `metosin/malli`      | Schema annotations on public fns (not enforced at runtime) |
| `polylith/clj-poly`  | Workspace tooling (`:poly` alias)                          |

## Polylith conventions

- `top-namespace` is `antithesis` (set in `workspace.edn`).
- Each component has `src/antithesis/<name>/interface.clj` (public) and
  `core.clj` (private).
- Tests live at `components/<name>/test/antithesis/<name>/interface_test.clj`
  and test only through the interface namespace.
- The `:dev` alias at the root `deps.edn` adds `development/src` to the
  classpath for REPL work; the `:poly` alias runs Polylith tooling.
