# Signing policy

Public Android releases use one persistent signing identity starting with the collision-free public baseline version 0.3.4.

Stable public identity:

- package: `com.personal.healthbridge.reinstall`
- SHA-256: `98:EC:08:EC:06:40:00:36:88:D3:C9:9A:60:8C:BA:45:C7:5C:0C:AE:A3:3E:63:D2:86:FC:64:0E:4F:45:42:92`
- SHA-1: `E3:1A:E1:24:F9:3D:06:F1:78:4F:AC:1F:1E:B6:24:1B:08:BF:7A:F2`

The private signing material is stored only in the GitHub Actions repository secret named `HEALTHBRIDGE_PUBLIC_SIGNING_BUNDLE_BASE64`. It must never be committed to the repository, release assets, logs or documentation.

Every stable release must verify the produced APK certificate against the fixed SHA-256 value above. A build signed by a different key is a release failure, because future in-place Android updates depend on keeping this identity unchanged.

The old private-repository application identity is intentionally not preserved. Public 0.3.4 uses a new application ID to avoid collisions with any old private package state. Public 0.3.4 and later keep the same application ID and signing certificate and remain update-compatible with each other.
