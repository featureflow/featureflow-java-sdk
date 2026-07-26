# Events, goal tracking, and server-driven SDK config

This document covers how `featureflow-java-sdk` reports evaluation and goal (conversion)
events back to Featureflow, and how the server can steer that behaviour at runtime. It's
the Java implementation of a contract shared across Featureflow's SDKs — the
authoritative spec is `testbed/SDK-CONFIG.md` (a git submodule pointing at
[featureflow-sdk-testbed](https://github.com/featureflow/featureflow-sdk-testbed)),
with `featureflow-node-sdk` as the reference implementation.

## Evaluate events are summarised, not raw

Every call to `.is()` / `.isOn()` / `.isOff()` / `.value()` on an `Evaluate` records an
impression, but the SDK does **not** queue one HTTP event per call. Instead
`EventsClientImpl` keeps one pending entry per `(featureKey, evaluatedVariant)` pair with
an impression count, and flushes summed counts on an interval (60 seconds by default).
This keeps event volume proportional to the number of distinct feature/variant
combinations in play, not the number of evaluations.

Each distinct user is attached to at most one summary entry per flush interval, so the
server still learns every user's attributes without the payload repeating the user on
every evaluation. A feature control carrying `trackEvents: true` (dormant until
server-side experimentation ships) instead attaches each distinct user **once per flag**
per interval — full per-(user, flag, variant) exposure fidelity for experiment analysis.

`evaluateAll(user)` — typically used to snapshot flags for bootstrapping a client-side
SDK — never records evaluate events; only the accessor methods on `Evaluate` do.

## Goal tracking

```java
featureflow.track("signup", user);                                  // no metric value
featureflow.track("checkout-value", user, 129.90);                  // numeric value
featureflow.track("purchase", user, Map.of("value", 5, "plan", "pro")); // value + custom data
```

`details` is one of:

- `null` — no metric value.
- A `Number` — the metric value.
- A `Map` — an optional numeric `"value"` entry plus any other custom fields, sent as
  `data`.

This mirrors the [OpenFeature tracking API](https://openfeature.dev/specification/sections/tracking)
shape, so an OpenFeature provider built on this SDK can forward `track()` calls
unchanged. Goal events are sent **raw** (never summarised) in the same flush batch as
summarised evaluate events — analysis joins them against exposures on the user id.

## Server-driven SDK config

The server can retune event/polling behaviour without an SDK release, via a small JSON
object delivered on endpoints the SDK already calls — no extra request is ever made for
it:

| Field | Type | Meaning |
|---|---|---|
| `eventsEnabled` | boolean | Master switch. `false` suspends event recording/sending (reversible) until a later config re-enables it. |
| `mode` | `"summary"` \| `"full"` \| `"off"` | `summary` (default) as described above; `full` records one event per evaluation with the user attached to every event; `off` records nothing. |
| `flushIntervalSeconds` | number (1–3600) | How often summarised events/goals are flushed. |
| `pollIntervalSeconds` | number (5–3600) | How often feature configuration is polled. |

Delivery channels:

1. The `X-Featureflow-Sdk-Config` response header on the features poll (both `200` and
   `304` responses — a long-running polling client mostly sees `304`s).
2. The response body of the events `POST`, on `200`.

Rules, applied field-by-field:

- **Absent field ⇒ keep current value.** The server may send a partial object.
- **Invalid value ⇒ ignore that field**, keep the rest (wrong type, unknown `mode`,
  out-of-range interval).
- **Unknown fields ⇒ ignored** — additive evolution, never a hard failure.
- **A local disable always wins.** An events client constructed with events disabled,
  or a features poller configured with `interval: 0` (disabled), is never re-enabled or
  rescheduled by server config. Likewise, once the events client has been permanently
  disabled by a `401`/`403` (see below), no server config can re-enable it.

## Responding to server signals on the events endpoint

- **`401` / `403`** — the API key is not authorized. Event sending is disabled for the
  remaining lifetime of the client (distinct from the reversible `eventsEnabled: false`
  suspension above).
- **`429`** — the batch is rejected. The SDK backs off for the response's `Retry-After`
  seconds (default 60 if absent), and merges the rejected batch back into whatever has
  been summarised since, so no impressions or goals are lost.
