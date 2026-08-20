# CarBroz Foundation V2

This is the fresh product-neutral foundation baseline for the independent Partner and Customer applications.

## Frozen principles

- Clean Architecture + MVI/UDF + SDUI + modular dependency inversion.
- Maximum reusable common Kotlin; thin Android/iOS/Desktop hosts.
- Adaptive UI is driven by current window/container constraints, posture and input mode, not device labels.
- SDUI will use decode -> validate -> compatibility -> normalize -> runtime IR -> bind -> render.
- Background scheduling is semantic in common code; WorkManager, Apple BackgroundTasks and Desktop policies are platform adapters.
- Public architecture APIs require meaningful KDoc.
- Relevant foundation code targets at least 90% meaningful line and branch coverage where tooling supports it.
- Positive, negative, boundary, concurrency, cancellation, timeout, retry, recovery and malformed-input tests are mandatory where applicable.
