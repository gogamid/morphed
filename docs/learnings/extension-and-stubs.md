# Extensions and compile-only stubs

The pattern behind `extensions/extension` and the `libs/rossmann-stub-5.11.1.jar` it compiles
against.

## What an extension is

An extension is **precompiled DEX**: `extensions/extension` is an Android library whose only
output is `extensions/extension.mpe` (declared in its `build.gradle.kts` via
`extension { name = "extensions/extension.mpe" }`). Patches merge it into the patched app
with

```kotlin
extendWith("extensions/extension.mpe")
```

and then inject small smali calls into the extension's classes:

```kotlin
PageViewModelAdapterConstructorFingerprint.method.addInstructions(
    0,
    """
        invoke-static {p1}, Lapp/gogamid/morphed/extension/rossmann/RossmannAutoClaim;->register(Lde/rossmann/app/android/business/coupon/ActivateCoupon;)V
    """.trimIndent(),
)
```

Keep patches thin: they capture instances and call into the extension; logic lives in Java
where it can be read, tested and reasoned about.

## You cannot compile against app classes

Rossmann's classes exist only in the APK. Solution: hand-written **stubs** under
`extensions/extension/stubs/`, compiled into `libs/rossmann-stub-<version>.jar`, consumed as

```kotlin
compileOnly(files("libs/rossmann-stub-5.11.1.jar"))
compileOnly("org.jetbrains.kotlin:kotlin-stdlib:2.3.0")
```

- Stubs are **never in the dex** (`compileOnly`, and the extension only depends on them for
  signatures). At runtime the same descriptors bind to the app's real classes.
- Stub initialisers never run; fields like `public final String l = null;` are just shapes.
- The jar name is pinned to the supported version so a version bump forces a rebuild.

`.gitignore` ignores `*.jar`, so the stub jar needs an explicit exception (already present):

```gitignore
*.jar
# Compile-only stubs of app classes the extension links against (see extensions/extension/stubs).
!extensions/extension/libs/*.jar
```

Rebuild:

```sh
javac -cp kotlin-stdlib-<version>.jar -d build/ \
    stubs/de/rossmann/app/android/models/coupon/Coupon.java \
    stubs/de/rossmann/app/android/business/coupon/ActivateCoupon.java
jar cf libs/rossmann-stub-5.11.1.jar -C build de
javap -p -classpath libs/rossmann-stub-5.11.1.jar de.rossmann.app.android.business.coupon.ActivateCoupon
```

## Calling the app's suspend function from Java

`ActivateCoupon.c(Coupon, ContinuationImpl)` is a Kotlin `suspend` function compiled with a
`ContinuationImpl` parameter. From Java you subclass the runtime class
`kotlin.coroutines.jvm.internal.ContinuationImpl`:

```java
static final class ClaimContinuation extends ContinuationImpl {
    ClaimContinuation() { super(TerminalContinuation.INSTANCE); }

    @Override public Object invokeSuspend(Object result) { return Unit.INSTANCE; }
    @Override protected void releaseIntercepted() { }
}

static final class TerminalContinuation implements Continuation<Object> {
    static final TerminalContinuation INSTANCE = new TerminalContinuation();
    @Override public CoroutineContext getContext() { return EmptyCoroutineContext.INSTANCE; }
    @Override public void resumeWith(Object result) { /* fire and forget */ }
}
```

Interop facts that cost time:

- `Continuation<Any?>` is **invariant**. Java sees `Continuation<Object>`, so a
  `Continuation<Unit>` terminal does not compile.
- `ContinuationImpl`'s constructor takes `Continuation<Object>` — that is the terminal above.
- `BaseContinuationImpl.resumeWith` on completion re-enters your `invokeSuspend`; returning
  `Unit.INSTANCE` is the whole lifecycle.
- Failures arrive as a *completed with failure* `Result`, not as a thrown exception. The
  terminal's no-op `resumeWith` swallows them — which is exactly the desired behaviour here:
  **silent refusals**, identical to a refused manual tap. Do not add logging that turns them
  into visible errors.

## Instance capture from a constructor

`PageViewModelAdapterImpl` receives the Hilt-provided `ActivateCoupon` as its first argument.
Inject at index `0` and read `p1`:

```kotlin
method.addInstructions(0, "invoke-static {p1}, …->register(…)V")
```

Constructor parameters are readable **before** the `super()` call; only `this` (`p0`) is not
usable yet. This is how the extension gets the app's real use case instead of constructing
one (which it cannot do anyway).

## Concurrency and runtime details

- Claims run on a single-thread `ExecutorService`; Room's suspend DAO calls dispatch
  themselves, so nothing blocks the main thread.
- `ConcurrentHashMap.newKeySet()` needs API 24+; the app's `minSdk` is 24, so it is fine.
- Dedupe per process (`Set<String> CLAIMED`) so repeated factory calls stay silent.
- Static `volatile ActivateCoupon` is registered once; `onCoupons` no-ops until it is set.

## Rossmann domain semantics encoded in the extension

From the real `Coupon` model and `CouponFactoryKt`:

| Member | Meaning |
|---|---|
| `coupon.q: boolean` | coupon is already in the wallet |
| `coupon.l: String` | EAN, the backend identifier |
| `coupon.R: PreviewState` | preview state; `NotPreview` (singleton `NotPreview.a`) = **available now**, built from `availableFrom` being in the past or absent |

Claim rule therefore:

```java
if (coupon.q) continue;                     // already in wallet
if (availableNow != coupon.R) continue;     // preview → do NOT mark attempted
if (ean == null || ean.isEmpty()) continue; // cannot be synced
if (!CLAIMED.add(ean)) continue;            // already tried this process
```

Preview coupons are deliberately **not** marked attempted, so they are claimed once they
become available and the factory runs again. Exclusive coupons and wallet limits are handled
by the app's own use case, whose refusals stay silent.
