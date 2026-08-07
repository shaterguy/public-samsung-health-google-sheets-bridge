# Build verification

Public baseline: Health Bridge Android `0.3.3` (`versionCode 9`, package `com.example.healthbridge`).

The public repository is considered build-verified only when GitHub Actions completes all of the following:

- Android unit tests
- unsigned release APK build
- restore of the persistent public signing identity from the repository secret
- release APK signing
- APK signature verification against the fixed public certificate
- package, version code and version name verification
- SHA-256 checksum generation
- GitHub Release publication
- release asset re-download and verification

The persistent public signing secret has been provisioned. The release workflow records a successful end-to-end verification in `.github/release-selftest-report.json`; that report, not secret presence alone, is the completion criterion. A separate observer records every stable-release conclusion in `.github/release-observed-result.json` so failures are not mistaken for incomplete runs.

Physical-device Health Connect permission and Google OAuth are separate runtime checks. Because public 0.3.3 uses a new signing certificate, its package and SHA-1 must be registered as an Android OAuth client before Google Sheets authorization on the installed public APK can be verified.

No device-specific health statistics, account identifiers, spreadsheet identifiers or OAuth tokens are stored in this verification document.
