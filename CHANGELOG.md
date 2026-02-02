## Version 1.0.0 — Starting a new plugin
**Release date:** 2026-01-13

## Version 1.1.0 — Modularization, Multi-Language Support & Storage Refactor
**Release date:** 2026-01-29

### Changed
- Extracted moderation messages from `config.yml` into a dedicated `moderation.yml`.
- Reorganized the project into a modular structure to support future expansions.
- Refactored the storage layer for improved reliability and extensibility.
- Updated deprecated events to modern Paper/Spigot APIs.

### Fixed
- Fixed `anti-spam.anti-repeat.max-history` not working as intended.
- Updated internal tests to reflect the new architecture and configuration layout.

### Added
- Implemented a multi-language system using `config.yml/locale.<lang>` files.
- Added built-in locales: `en_US`, `es_LA`, `pt_BR`, `de_DE`, `nl_NL`.
- Introduced `anti-spam.anti-repeat.cooldown` to control repeated-word cooldowns.
- Added MySQL and MariaDB support to the storage system.

## Version 1.1.1 — Anti-flood fixed
**Release date:** 2026-02-02

### Changed
- 'notifications' module moved from moderation.yml to config.yml
- Redundant code removed

### Fixed
- Anti-flood was not detecting
