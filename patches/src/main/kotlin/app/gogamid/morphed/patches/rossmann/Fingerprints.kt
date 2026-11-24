package app.gogamid.morphed.patches.rossmann

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * The static factory of CouponDisruptorState, verified against Rossmann 5.11.1
 * (versionCode 421000110).
 *
 * The factory turns a coupon list into the app's "coupons present" disruptor
 * state. It is reached from the coupon nag helper, product search, the cart,
 * order history and product details, so every context that presents coupons
 * passes through here.
 *
 * The app obfuscates method and field names but keeps class names, so this
 * fingerprint anchors on the class, the full parameter list and the two
 * null-check string constants at the top of the method.
 */
object CouponDisruptorCreateFingerprint : Fingerprint(
    definingClass = "Lde/rossmann/app/android/models/coupon/CouponDisruptorState\$Companion;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    parameters = listOf("Ljava/util/List;", "Ljava/lang/String;", "Lkotlin/jvm/functions/Function1;"),
    returnType = "Lde/rossmann/app/android/models/coupon/CouponDisruptorState;",
    filters = listOf(
        string("productEan"),
        string("findActivatedCoupon"),
    ),
)

/**
 * The constructor of the pages view model adapter, verified against Rossmann
 * 5.11.1 (versionCode 421000110).
 *
 * Hilt hands the adapter the app's own ActivateCoupon use case as its first
 * argument. The patch registers that instance in the extension, so the
 * extension can drive the app's own activate action.
 */
object PageViewModelAdapterConstructorFingerprint : Fingerprint(
    definingClass = "Lde/rossmann/app/android/di/PageViewModelAdapterImpl;",
    name = "<init>",
    // Declared parameters must match the method's full parameter list. The class
    // keeps its name, the first three arguments identify the constructor, and the
    // remaining domain types are declared as objects so they survive obfuscation.
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
