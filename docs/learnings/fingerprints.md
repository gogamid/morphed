# Fingerprint rules that cost us a release

Everything here was learned the hard way on Rossmann 5.11.1 (`versionCode 421000110`).
The repo's fingerprints live in
`patches/src/main/kotlin/app/gogamid/morphed/patches/rossmann/Fingerprints.kt`.

## The hard rule: parameters are matched as a whole list

From the morphe-patcher docs:

> Declared parameters must have the same number of parameters as the method.

A **partial** parameter list never matches — not "match a prefix", not "match these
somewhere". If the target takes 7 parameters, you declare 7 entries.

This is exactly what broke v1.0.0: the build was fine, the release was published, and
applying the bundle died with

```
Failed to match the fingerprint: PageViewModelAdapterConstructorFingerprint
```

The constructor has **7** parameters; the fingerprint declared only the first **3**. Fix:
declare all seven — the three identity-carrying descriptors in full, the remaining four as
object wildcards:

```kotlin
object PageViewModelAdapterConstructorFingerprint : Fingerprint(
    definingClass = "Lde/rossmann/app/android/di/PageViewModelAdapterImpl;",
    name = "<init>",
    parameters = listOf(
        "Lde/rossmann/app/android/business/coupon/ActivateCoupon;",
        "Lde/rossmann/app/android/business/coupon/DeactivateCoupon;",
        "Lde/rossmann/app/business/repository/CouponRepository;",
        "L",
        "L",
        "L",
        "L",
    ),
)
```

`"L"` costs nothing in precision here: the first three arguments already pin the
constructor, and the rest are domain types that may be obfuscated.

## How each parameter string is compared

| Declaration | Comparison | Matches |
|---|---|---|
| `Lde/rossmann/app/android/models/coupon/Coupon;` (full `L…;`) | equals | exactly that class |
| `I`, `Z`, `J`, `F`, `D`, `B`, `S`, `C` (primitive) | equals | exactly that primitive |
| `[Ljava/lang/String;` (starts with `[`) | prefix | any array whose descriptor starts with it |
| `;Ljava/lang/String;` (ends with `;`) | suffix | descriptors ending in that type |
| anything else, e.g. `L` | contains | any object type (`L…;`) |

Two rules of thumb: use the full descriptor when the class survives obfuscation, and a bare
`L` when it does not. Never leave a slot out.

## Constructors

- Constructors match with `name = "<init>"` (static initialisers with `"<clinit>"`).
- `name = "<init>"` **cannot** be combined with `isStatic` — the fingerprint is rejected.
- For regular methods prefer `isStatic = true` / `isStatic = false` over spelling out
  `accessFlags`. Visibility, `final` and `synthetic` drift between releases; staticness
  usually does not. Use `accessFlags` only when the flags themselves are the signal:

```kotlin
object CouponDisruptorCreateFingerprint : Fingerprint(
    definingClass = "Lde/rossmann/app/android/models/coupon/CouponDisruptorState\$Companion;",
    isStatic = true,
    parameters = listOf("Ljava/util/List;", "Ljava/lang/String;", "Lkotlin/jvm/functions/Function1;"),
    returnType = "Lde/rossmann/app/android/models/coupon/CouponDisruptorState;",
    filters = listOf(
        string("productEan"),
        string("findActivatedCoupon"),
    ),
)
```

Why the switch: the first released version declared
`accessFlags = listOf(PUBLIC, STATIC, FINAL)`, but the shipped method is only
`public static` — no `final`. `accessFlags` is an **exact** comparison, so declaring an
extra flag fails the match just as surely as a missing parameter does. The patcher
reports `Failed to match the fingerprint: …CouponDisruptorCreateFingerprint`, which pointed
straight at the flags after the parameter fix had moved the failure there. `isStatic = true`
expresses the only characteristic that matters.

## String filters are ordered instruction filters

`filters = listOf(string("a"), string("b"))` means: an instruction loading `"a"`, *then
later* an instruction loading `"b"`, in that order. They are not "contains both".

The strongest cheap pattern in Kotlin code is the `Intrinsics` null-check pair at the top of
a method. `CouponDisruptorCreateFingerprint` anchors on `"productEan"` (the `require` message
for the EAN argument) followed by `"findActivatedCoupon"` (the log/require message further
down), which pins one method among the companion's many factories.

## Rossmann's R8 keeps class names, renames members

Observed on 5.11.1:

- Class names are **kept**: `de.rossmann.app.android.models.coupon.Coupon`,
  `...business.coupon.ActivateCoupon`, `...di.PageViewModelAdapterImpl`,
  `CouponDisruptorState$Companion`.
- Methods are renamed: the suspend activate action is `ActivateCoupon.c(Coupon, ContinuationImpl)`.
- Fields are renamed: `Coupon.q:Z` (in wallet), `Coupon.l:String` (EAN),
  `Coupon.R:PreviewState`, and `NotPreview.a` for the singleton.

Consequence — anchor on:

1. `definingClass` (stable), plus
2. the **full** parameter signature, plus
3. distinctive string constants,

and never on a method or field name. The injected smali and the extension stubs do use the
renamed members (`c`, `q`, `l`, `R`), so they are pinned to the compatibility version in
`shared/Constants.kt` and must be revisited with every supported-version bump.

See [apk-analysis-workflow.md](apk-analysis-workflow.md) for how to confirm those names in
smali, and [extension-and-stubs.md](extension-and-stubs.md) for how the stubs encode them.
