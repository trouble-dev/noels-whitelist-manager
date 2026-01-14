# Whitelist Management Plugin

A Hytale server plugin that provides a visual UI for managing the server whitelist.

## Features

- View all whitelisted players
- Add players to whitelist (online players by username, offline by UUID)
- Remove players from whitelist
- Enable/disable whitelist enforcement
- Real-time status display
- **Pending Requests**: Capture rejected connection attempts for easy one-click whitelisting

## Usage

Use the `/wl` command in-game to open the whitelist management UI.

### Adding Players

There are two ways to add players:

1. **Online Players**: Enter the player's username and click "Confirm"
2. **Pending Requests**: When a player tries to connect but is rejected due to whitelist, their attempt is logged. You can then add them with one click from the "Pending Requests" section.

## Known Limitations

### UUID Resolution (Chicken-Egg Problem)

The Hytale server's `AuthUtil.lookupUuid()` method is **deprecated** and generates **fake UUIDs** for offline players using an offline-mode hash (`UUID.nameUUIDFromBytes(("NO_AUTH|" + username)...)`). This means:

- **You cannot add offline players by username alone** - the generated UUID won't match their real UUID
- All official Hytale whitelist commands (`/whitelist add <name>`) have the same limitation
- The only reliable way to get a player's real UUID is when they attempt to connect

### Workaround: Pending Requests System

This plugin implements a **pending requests** feature to work around this limitation:

1. Enable the whitelist
2. Tell the player to attempt to connect
3. Their connection will be rejected, but their **real UUID and username** are captured
4. Open the whitelist UI (`/wl`) and find them in the "Pending Requests" section
5. Click "ADD" to whitelist them with the correct UUID

This approach captures the real UUID directly from the `PlayerSetupConnectEvent`, which contains both the authenticated UUID and username.

### Why This Matters

If you add a player using a fake UUID (from `AuthUtil.lookupUuid()`), the whitelist entry won't match their real UUID, and they still won't be able to join.

## Building

```bash
./gradlew jar
```

Output: `build/libs/WhitelistPlugin-1.0.0.jar`

## CI/CD

GitHub Actions baut automatisch bei:
- **Push auf `main`** → Build + JAR als Artifact
- **Pull Requests** → Build-Validierung
- **Manual Trigger** → Build + GitHub Release mit JAR

### Neues Release erstellen

1. Gehe zu [Actions](../../actions) → "Build and Release"
2. Klicke "Run workflow"
3. Wähle Version-Typ:
   - `patch` → 1.0.0 → 1.0.1 (Bugfixes)
   - `minor` → 1.0.0 → 1.1.0 (Neue Features)
   - `major` → 1.0.0 → 2.0.0 (Breaking Changes)
4. Klicke "Run workflow"

Das Release erscheint dann unter [Releases](../../releases) mit der JAR zum Download.

## Installation

Copy the JAR file to your Hytale server's `mods/` folder.

## Data Files

- `whitelist.json` - Server whitelist (managed by Hytale)
- `whitelist_pending.json` - Pending connection attempts (managed by this plugin)

## Requirements

- Hytale Server (Early Access)
- Java 21
