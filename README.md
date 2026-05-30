# antithesis-sdk-clojure

Unofficial Clojure SDK for the [Antithesis](https://antithesis.com) continuous
testing platform. Instruments Clojure applications to communicate assertions,
life-cycle events, and controlled randomness to the Antithesis test environment.

## Installation

Add the dependency to your `deps.edn`:

```clojure
{:deps {com.antithesis/sdk-clojure {:mvn/version "LATEST"}}}
```

The JVM flag `--enable-native-access=ALL-UNNAMED` is required when running
inside Antithesis.

## Namespaces

| Namespace              | Purpose                        |
| ---------------------- | ------------------------------ |
| `antithesis.assert`    | Assertion macros               |
| `antithesis.lifecycle` | Set-up and event signals       |
| `antithesis.random`    | Platform-controlled randomness |

## Usage

### Assertions

All assertions take a `message` string (the property name) and a `details` map
(arbitrary structured context).

```clojure
(require '[antithesis.assert :as a])

;; Must be true every time this code is reached
(a/always (= status :ok) "Response is ok" {:status status})

;; Must be true every time, but passes if never reached
(a/always-or-unreachable (valid? x) "Input is valid" {:x x})

;; Must be true at least once across the test run
(a/sometimes (= role :admin) "Admin path exercised" {:user user})

;; Reachability
(a/reachable "Error handler reached" {:code error-code})
(a/unreachable "Dead branch" {})
```

Numeric and boolean guidance variants emit additional signals to help the
platform converge:

```clojure
(a/always-greater-than latency-ms 0 "Latency is positive" {:latency latency-ms})
(a/sometimes-less-than queue-depth 100 "Queue drains" {:depth queue-depth})
(a/always-some {:connected? connected? :healthy? healthy?} "System ok" {})
(a/sometimes-all {:a? a? :b? b?} "Both paths taken" {})
```

### Lifecycle

```clojure
(require '[antithesis.lifecycle :as lc])

;; Call once when your service is ready to receive test traffic
(lc/setup-complete! {:version "1.2.3"})

;; Emit a named structured event
(lc/send-event! "payment-processed" {:amount 42.00 :currency "USD"})
```

### Randomness

```clojure
(require '[antithesis.random :as r])

;; Returns a long controlled by the platform (or java.util.Random outside
;; Antithesis)
(r/get-random)

;; Returns a randomly chosen element from a collection
(r/random-choice [:a :b :c])
```

## Handler selection

The SDK selects an output handler once, at first use:

1. **FfiHandler** when running inside Antithesis (JNI library present)
2. **LocalHandler** when `ANTITHESIS_SDK_LOCAL_OUTPUT` is set to a file path
   (writes JSONL)
3. **NoopHandler** otherwise (safe to use in production; `get-random` falls back
   to `java.util.Random`)

## Development

This repository is a [Polylith](https://polylith.gitbook.io) monorepo.

```bash
# Validate workspace
clj -M:poly check

# Run tests
clj -M:poly test :dev

# Show component overview
clj -M:poly info
```

### Building and installing locally

The build tooling lives in `projects/antithesis-sdk/`. Run these from the
repository root:

```bash
cd projects/antithesis-sdk

# Clean, compile, and package a JAR
clj -T:build jar-all

# Install the JAR to your local ~/.m2 cache
clj -T:build jar-install
```

After `jar-install`, add the library to your project's `deps.edn`:

```clojure
{:deps {dev.tomwaddington/antithesis-sdk-clj {:mvn/version "0.0.0-SNAPSHOT"}}}
```

Set `VERSION` before building to produce a versioned artifact:

```bash
VERSION=1.2.3 clj -T:build jar-all
VERSION=1.2.3 clj -T:build jar-install
```

## License

See [LICENSE](LICENSE).
