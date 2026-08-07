# Public Android OAuth setup

The public APK uses a new persistent signing identity.

- Package name: `com.example.healthbridge`
- SHA-1 certificate fingerprint: `E3:1A:E1:24:F9:3D:06:F1:78:4F:AC:1F:1E:B6:24:1B:08:BF:7A:F2`
- SHA-256 certificate fingerprint: `98:EC:08:EC:06:40:00:36:88:D3:C9:9A:60:8C:BA:45:C7:5C:0C:AE:A3:3E:63:D2:86:FC:64:0E:4F:45:42:92`

Google Cloud must contain an Android OAuth client matching the package name and SHA-1 above before the public APK's Google Sheets authorization can be considered operationally verified.

The OAuth client ID itself is not embedded in this repository. Google Play services resolves the installed Android application's package and signing certificate during authorization.
