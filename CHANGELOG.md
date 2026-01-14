# Changelog

All notable changes to this project will be documented in this file.

## [1.2.0] - 2025-01-14

### Added
- Visible scrollbars with textures for PlayerList and PendingList
- ScrollbarStyle now includes Background, Handle, HoveredHandle, and DraggedHandle textures

## [1.1.0] - 2025-01-14

### Added
- Scrolling support for player and pending request lists (`LayoutMode: TopScrolling`)
- Permission requirement `whitelist.manage` for `/wl` command (OPs have access by default)
- Dynamic JAR versioning - filename now reflects release version

## [1.0.0] - 2025-01-14

### Added
- Initial release
- Whitelist management UI accessible via `/wl` command
- View all whitelisted players
- View pending connection requests (rejected players)
- Add/remove players from whitelist
- Enable/disable whitelist enforcement
- Status display showing whitelist state
