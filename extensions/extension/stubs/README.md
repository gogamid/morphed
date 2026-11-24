# Rossmann stubs (compile-only)

Stubs of the Rossmann app classes that the extension calls into, verified
against Rossmann 5.11.1 (versionCode 421000110):

- `de.rossmann.app.android.models.coupon.Coupon` — the fields `l`, `q` and `R`
- `de.rossmann.app.android.models.coupon.Coupon$PreviewState$NotPreview`
- `de.rossmann.app.android.business.coupon.ActivateCoupon` — the suspend
  activate action `c(Coupon, ContinuationImpl)`

The single-letter names are the app's R8 output. The patch fingerprints pin
this exact build, so the names only change together with a compatibility
bump in `patches/.../shared/Constants.kt`.

These classes are never shipped. They are compiled into
`libs/rossmann-stub-5.11.1.jar`, which the extension module consumes as
`compileOnly`, so the extension dex never contains them and the calls bind
against the app's real classes at runtime.

## Rebuilding the jar

```
javac -cp kotlin-stdlib-<version>.jar -d build/ \
    stubs/de/rossmann/app/android/models/coupon/Coupon.java \
    stubs/de/rossmann/app/android/business/coupon/ActivateCoupon.java

jar cf libs/rossmann-stub-5.11.1.jar -C build de
```

The kotlin-stdlib jar is only needed for the `ContinuationImpl` parameter
type of the `ActivateCoupon` stub and can be any recent version.
