# Project continuity for Android Apps / SaúdePrática

When working on **SaúdePrática** in this repository, read these files first:
1. [SaúdePrática/INSTRUCOES_DE_CONTINUIDADE.md](SaúdePrática/INSTRUCOES_DE_CONTINUIDADE.md)
2. [SaúdePrática/PUBLICAR_APK_GITHUB.md](SaúdePrática/PUBLICAR_APK_GITHUB.md)
3. [SaúdePrática/README.md](SaúdePrática/README.md)

The project owner explicitly requires:
- Continue across ChatGPT conversations in the same project, **searching the existing repository and Project/Library for code, APKs, graphics and the original Android keystore before asking for files again**.
- **Always publish signed updates to GitHub Releases**, never treat a build artifact, a ZIP or a downloaded APK as the completed release.
- After publishing, confirm latest tag, release status, public `SaudePratica.apk` asset, SHA-256, and permanent download link.
- Keep the original Android certificate and package ID for in-place upgrades; never commit private keystores, passwords, private data or patient files.

The signed-delta publication method was used successfully for **v1.9.77, v1.9.78, v1.9.79 and v1.9.80**, and is documented in the files linked above. Use it when the Actions runner lacks the original signing secret. Do not assert publication until verified.

## Mandatory branded artwork gate
- Restore `saude_background.png`, `saude_icon.png` and `saude_splash.png` using `MediAgenda/v98_overrides/branding/*.png.b64` and the v98 build patch. Assets were provided by the project owner; never request them again without first checking repository and Project/Library.
- Before every Release, run `MediAgenda/tools/verify_graphics.py`, verify the three branded PNGs are inside the **compiled APK**, and check original signing certificate. Do not publish a build with placeholder graphics.
- Latest verified published release: v1.9.80 (26 Oct 2026); fixed asset `SaudePratica.apk`.
