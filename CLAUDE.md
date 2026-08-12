# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

The Featureflow **server-side Java SDK**, published to Maven Central as `io.featureflow:featureflow-java-sdk`. It polls (or streams via SSE) feature configuration from the Featureflow API, evaluates variants locally per user, and reports evaluation/goal events back. Its wire protocol and variant-bucketing algorithm are shared across Featureflow's SDKs (Node, browser JavaScript, Go, Python, Ruby, .NET, …) and must stay in sync — cross-SDK conformance scenarios live in the `featureflow-sdk-testbed` git submodule (`testbed/`).

## Commands

```bash
git submodule update --init           # required once: pulls in testbed/gherkin
./mvnw clean package                  # build + full test suite
./mvnw test                           # tests only
./mvnw test -Dtest=ClassName           # single JUnit test class
./mvnw test -Dtest=CucumberTest        # the full cucumber suite
```

Java 17+ (`java.version` in `pom.xml`). Plain Maven — no lockfile-manager quirks.

Publishing to Maven Central is via GitHub Actions (`release.yml`, migrated off CircleCI 2026-08-11): create a GitHub release with a **bare** tag matching the pom version (`1.4.0`, no v prefix); a `workflow_dispatch` dry-run builds and signs without uploading. See `ops/development/sdk-publishing.md` in the workspace for the full flow and secrets layout.

## Testing

Two styles coexist:

- **Plain JUnit** (`OperatorTest`, `RuleMatchesTest`, `RuleVariantsTest`, `FeatureControlTest`, `FeatureManagerTest`, `JsonValueTest`) for pure evaluation-logic unit tests.
- **Cucumber** (`cucumber/CucumberTest` + `cucumber/stepdefs/`), driven by Gherkin scenarios shared across SDKs via the `testbed` submodule. Only a subset of the testbed's `.feature` files are wired up here — see the `<testResource><includes>` list in `pom.xml`. Add a new `<include>` there (and matching step definitions) when picking up a new shared scenario file.
- `TestAccessor` (`src/test/java/io/featureflow/client/TestAccessor.java`) exposes a few package-private internals (feature-control cache injection, event-handler access) to test code without reflection — extend it rather than widening production-code visibility just for tests.

`json_value.feature` in the testbed is tagged `@json-value` and still not wired up as Cucumber — its final scenario assumes a raw per-evaluation event shape (`{featureKey, evaluatedVariant, user}`) that doesn't match this SDK's summarised evaluate events. `Evaluate.jsonValue()`/`jsonValue(JsonElement defaultValue)` (`FeatureflowClient.java`) implements the same `jsonValue()` contract as the JS-family SDKs — a JSON config payload carried per-variant, resolved via `FeatureControl.variants`/`Variant.value` — with equivalent coverage in `JsonValueTest` instead.

## Architecture

Package layout under `io.featureflow.client`:

- `FeatureflowClient` — the public entry point (`FeatureflowClient.builder(apiKey).build()`). Holds the feature-control cache, the polling/streaming client, and the events client. `evaluate(key, user)` returns an `Evaluate` (`is()`/`isOn()`/`isOff()`/`value()` — the first call after construction is what records an evaluate event); `evaluateAll(user)` snapshots every feature **without** recording events; `track(goalKey, user, details)` records a goal event.
- `core/` — transport and background workers:
  - `RestClientImpl` / `RestClient` — HTTP (Apache HttpClient 5) for feature registration and event posting.
  - `FeatureflowPollingClient` — default transport. Polls with `If-None-Match` ETag caching; honours a server-driven `pollIntervalSeconds` (delivered via the `X-Featureflow-Sdk-Config` response header) — a locally disabled poller (`interval: 0`) is never re-enabled by server config.
  - `FeatureControlStreamClient` — SSE alternative (`config.withUseStreaming(true)`), OkHttp-based.
  - `EventsClientImpl` — summarises evaluate events client-side (one entry per `(featureKey, evaluatedVariant)` with an impression count, flushed on an interval) instead of sending one raw event per evaluation. Goal/track events are sent raw in the same batch. Honours server-driven config (`eventsEnabled` / `mode` / `flushIntervalSeconds`) delivered via the same header or the `/events` response body, and responds to `401`/`403` (permanent disable for the client's lifetime) and `429` (backoff + requeue) from the events endpoint. See `docs/EVENTS.md` for the full behaviour.
- `model/` — wire/data types: `FeatureControl` (a feature's rules for one environment), `Rule` / `Audience` / `Condition` / `VariantSplit` (targeting), `Variant`, `Event` (evaluate/goal wire shapes), `Feature` (code-registered failover).

**Variant bucketing must not change independently of the other SDKs** — see `Rule.getHash()` / `getVariantValue()` and the shared `bucketing.feature` / `rules.feature` scenarios.

## Further reading

- `docs/EVENTS.md` — event summarisation, goal tracking, and the server-driven SDK config contract, in depth.
