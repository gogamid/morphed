# Morphed

Morphe patches for German apps. ⚠️ Only patches — no proprietary code or rebuilt APKs. ⚠️

<a href="https://morphe.software/add-source?github=gogamid/morphed">
  <img src=".github/assets/add-to-morphe.svg" alt="Add to Morphe" height="48">
</a>

Or manually add **https://github.com/gogamid/morphed** as a patch source in Morphe.

## ⚠️ Warning

- **No affiliation.** Morphed is an independent community project. It is not affiliated
  with, endorsed by, or supported by Rossmann, dm-drogerie markt, or the Morphe project.
- **Trademarks.** Rossmann, Mein dm and all other product names and logos belong to their
  respective owners. They are used here only to say which apps the patches work with.
- **No app files.** This repository hosts only patch code. It does not contain proprietary
  code or rebuilt APKs. You must get the app from an official source yourself.
- **Your responsibility.** Modifying an app may break its terms of service or its
  functionality. You are responsible for your use of these patches.
- **No warranty.** These patches come without any warranty, as set out in sections 15
  and 16 of the GNU General Public License v3.0.

## 🚀 Quick start

1. Install Morphe from [morphe.software](https://morphe.software).
2. Open the [Add to Morphe](https://morphe.software/add-source?github=gogamid/morphed) link,
   or add `https://github.com/gogamid/morphed` manually as a patch source.
3. Pick the app you want to patch.
4. Select the patches you want.
5. Patch the app.

## 🩹 Patches list

<!-- PATCHES_START -->

<!-- Do not modify this section by hand. The patch list is generated when release.yml creates a new release.
     
     This section shows one row for each app. The details of each patch are in PATCHES.md,
     which is generated at the same time.

     If you wish to manually keep this list updated then remove the PATCHES_START and PATCHES_END 
     comment blocks entirely. -->

#### A list of your patches will automatically be shown here after your first patches release is created.

&nbsp;
<!-- PATCHES_END -->

## 🧑‍💻 Development

Build the patch bundle locally with:

```sh
./gradlew buildAndroid
```

The built patches file is written to `patches/build/libs/patches-*.mpp`.

The `app.morphe.patches` Gradle plugin is resolved from Morphe's GitHub Packages registry,
so local builds need credentials: set the `gpr.user` and `gpr.key` Gradle properties
(for example in `~/.gradle/gradle.properties`), or provide the `GITHUB_ACTOR` and
`GITHUB_TOKEN` environment variables.

Releases are automated from `main` with semantic-release using
[conventional commit](https://www.conventionalcommits.org/) messages:

- `feat:` → minor release
- `fix:`, `perf:`, `bump:` → patch release
- `chore:` → no release

The `dev` branch produces pre-releases.

## 📜 License

Morphed Patches are licensed under the [GNU General Public License v3.0](LICENSE).
