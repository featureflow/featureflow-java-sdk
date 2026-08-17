# Change log

## [1.4.1] - 2026-08-17
### Changed
- The published jar now targets Java 8 again (1.3.2 and 1.4.0 were inadvertently compiled
  for Java 17), so legacy Java 8 services can compile against and load the SDK. No API or
  behaviour changes; the application tag support from 1.4.0 is unchanged.

## [1.4.0] - 2026-08-12
### Added
- `withApplication(String)` on `FeatureflowConfig.Builder` (or the `FEATUREFLOW_APPLICATION`
  environment variable): name this workload (e.g. `checkout-api`) and the SDK sends it as
  the `X-Featureflow-Application` header on every request — features polls, event posts,
  feature registration and the SSE stream — so the Featureflow dashboard can attribute SDK
  usage and flag evaluations per application (Admin → SDKs, and the "Evaluated by" panel on
  a feature's statistics tab). The value is a slug (lowercase `[a-z0-9._-]`, max 64 chars);
  case is forgiven, anything else invalid is dropped with a warning and no header is sent.

## [1.3.2] - 2026-08-11
- First release published from GitHub Actions (migrated off CircleCI). No library changes.
