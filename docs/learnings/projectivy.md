# Projectivy Launcher: premium and preset import

Target: **Projectivy Launcher 4.73** (`com.spocky.projengmenu`, versionCode 98, TV).

## Premium unlock

The patch is adapted from `johninnld/nl-morphe-patches` (GPL-3), which in turn adapted
`saieshshirodkar/saiesh-morphe-patches` for 4.71.

Three edits in `PTApplication`:

1. `f()Z` — the wrapper around the native `vs(Z)Z` licensing call. It returns `true` unconditionally.
2. `<init>` — the premium `StateFlow` starts with `Boolean.FALSE` (false means unlocked), so every
   feature gate reads unlocked. Re-applying the patch is safe: a build that already reads `FALSE`
   is accepted.
3. `d(Z)V` — the billing callback setter. `p1` is forced to `0` so an unpaid or expired response
   cannot lock features again.

Distribution builds of 4.73 exist with different R8 output (for example the state field is
`Lze4;` in one build and a different class in another), so the setter is pinned by class, name and
signature instead of a field-type check.

## Theme preset import fix

Bundled presets live in `res/raw/theme_*.pltheme`, which are ZIP archives containing
`appearance.plbackup`. Import flow:

`ImportThemeActivity` -> `ImportSettingsActivity.t()` -> `yi3.l(Uri)` -> `os1.B(context, uri)` ->
`y71.b(byte[])` -> AES decrypt.

`os1.B` returns the **whole ZIP**, and `y71.b` decrypts its input as
`key material (16) | IV (16) | AES-CBC ciphertext`. A ZIP is not a valid block-aligned blob, so
decryption throws `IllegalBlockSizeException: WRONG_FINAL_BLOCK_LENGTH`; the UI reports
"An error occurred while retrieving config file".

The `Fix theme preset import` patch prepends an unwrap in `y71.b([B)[B`:

```smali
invoke-static {p0}, Lapp/gogamid/morphed/extension/projectivy/ThemeArchive;->unwrap([B)[B
move-result-object p0
```

`ThemeArchive.unwrap` returns the `appearance.plbackup` entry when the input starts with a ZIP
local-file header and falls back to the original bytes otherwise, so plain settings backups keep
working.

## Fingerprint gotcha

jadx displays classes in the default package as `defpackage.*`. The real descriptor has no
package: `Ly71;`, not `Ldefpackage/y71;`. Matching the jadx display name fails with
"Failed to match the fingerprint".

## Status

Patch application is verified against the published 4.73 APK with the Morphe CLI. On-device
verification of preset import and premium unlock is pending.
