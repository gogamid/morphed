# Mein dm: coupon activation lives in Hermes

Target: Mein dm Germany **6.23.0** (`de.dm.meindm.android`, version code `194649`, APKM).
These observations come from local static inspection, not a signed-in test. No Mein dm patch
is exposed in Morphe Manager yet.

## Where to hook

- Android's `CouponCenterFragment` hosts the React Native screen. It is not the activation
  implementation. The app ships `assets/index.android.bundle` as Hermes bytecode version 98.
  The bundle SHA-256 is `b39647e24a5e8eeab89098378a212d575ee9f540428d5adbeb723af7dfd8024c`.
- In a local `hermes-dec` disassembly, `CouponCenterScreen` (#32214) and
  `useCouponCenterScreen` (#32186) identify the screen and its list lifecycle. The latter reads
  `useCoupons`, fetch state, and the resulting lists, and manages refresh and lifecycle effects.
- `useOnActivationButtonPressed` (#32072) constructs **separate** activate and deactivate
  closures and checks `activation.activated` for dm coupons. The activation-only closure
  (#54605) reaches `_activateCoupon` (#54606) and its async body (#68865). Do not auto-call the
  toggle handler: a stale render could otherwise turn off a coupon.
- The button flow checks `stammdaten.dmMed` in tracking and presentation. That does not yet
  establish which dm-med items are eligible or whether all use the same activation path.
- The button handler emits analytics and uses a snackbar. Do not count an analytics event,
  a closed dialog, or an awaited button press as confirmation. Verify the activate-only
  result and an inactive-to-active transition for the same coupon in the app's state.

Function IDs are observations for this exact bundle, not cross-version fingerprints.
`hermes-dec` warns on version 98, so corroborate opcodes and offsets before writing any code.

## Patch and validation plan

1. Locate a version-guarded Hermes hook in the **foreground** Coupons screen, after the
   list loads. Confirm the activation input, eligibility, and success contracts before wiring it.
2. Use the original activation-only action and run one coupon at a time. Recheck the latest
   state before each activation. Skip already active coupons and anything the app does not
   offer to activate; never change account or eligibility rules. Keep dm-med out of the
   first supported scope unless its path is verified separately.
3. Count only confirmed new activations. Hook Android notifications separately, respecting
   the user's app and channel settings. The stock manifest already declares `POST_NOTIFICATIONS`.
4. Build only the patch bundle in GitHub Actions. Apply it locally to the exact APKM, check
   that all splits are intact, then test activation and notification on a signed-in phone.
   Do not commit the stock or modified Hermes bundle, disassembly, or APKs.
