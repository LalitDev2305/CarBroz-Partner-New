# CarBroz Foundation V2

This is the fresh product-neutral foundation baseline for the independent Partner and Customer applications.

## Frozen principles

- Clean Architecture + MVI/UDF + SDUI + modular dependency inversion.
- Maximum reusable common Kotlin; thin Android/iOS/Desktop hosts.
- Adaptive UI is driven by current window/container constraints, posture and input mode, not device labels.
- SDUI uses decode -> validate -> compatibility -> normalize -> runtime IR -> bind -> render.
- Splash is static; every post-Splash screen is backend-driven through the canonical dynamic runtime contract.
- Backend flow never maps to client-known business destinations such as Login, OTP, Dashboard, Booking, KYC, Profile or Earnings.
- Background scheduling is semantic in common code; WorkManager, Apple BackgroundTasks and Desktop policies are platform adapters.
- Public architecture APIs require meaningful KDoc.
- Relevant foundation code targets at least 90% meaningful line and branch coverage where tooling supports it.
- Positive, negative, boundary, concurrency, cancellation, timeout, retry, recovery and malformed-input tests are mandatory where applicable.

## Architecture documents

- `MASTER-ARCHITECTURE-CONSTITUTION.md` — overall architectural constitution.
- `17-phase-17-foundation-governance.md` — foundation publication/governance boundary.
- `18-dynamic-runtime-architecture.md` — frozen post-Splash backend-driven runtime, navigation, binding, action, restoration, realtime/background and sync contract.
