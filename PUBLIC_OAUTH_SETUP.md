# Public Google authorization

Public Health Bridge keeps one persistent application identity for in-place updates.

- Current version: `0.3.5`
- Package name: `com.personal.healthbridge.reinstall`
- SHA-1 certificate fingerprint: `E3:1A:E1:24:F9:3D:06:F1:78:4F:AC:1F:1E:B6:24:1B:08:BF:7A:F2`
- SHA-256 certificate fingerprint: `98:EC:08:EC:06:40:00:36:88:D3:C9:9A:60:8C:BA:45:C7:5C:0C:AE:A3:3E:63:D2:86:FC:64:0E:4F:45:42:92`

## Version 0.3.5 authorization path

Version 0.3.5 no longer uses the Android `AuthorizationClient` flow that caused the account-selection result to remain unapproved on the collision-free public package.

The app now:

1. opens the Google account picker,
2. requests a Google Sheets scoped access token for the selected Google account through Google Play services,
3. launches Google's recoverable consent screen when account interaction is required,
4. marks Google Sheets as connected only after a real access token is returned,
5. stores only the selected account name and connection state in encrypted app settings; access tokens are not persisted by the app.

No OAuth client ID, client secret, Google Cloud project ID, or signing-key material is embedded in this repository or requested from the user during app setup.

Runtime Google account authorization still depends on Google Play services and the selected Google account. CI can verify compilation, APK signing, package identity and installation, but real account authorization must be confirmed on a device with a Google account.
