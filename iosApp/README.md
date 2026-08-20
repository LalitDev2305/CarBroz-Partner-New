# CarBroz Partner iOS Host

The native iOS/Xcode host will use bundle identifier `com.carbroz.partner` and link the `CarBrozShared` framework produced by `:app:shared`.

Only unavoidable native iOS bootstrap, entitlements, background modes, notification hooks and provider integrations belong here. Reusable Kotlin architecture remains in KMP modules.
