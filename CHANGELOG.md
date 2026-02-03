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

## Version 1.2.0
**Release date:** 2026-02-03

### Changed
- Refactored the project structure by moving all word-related configuration files from the `words` directory into the new `moderation` directory to improve modularity and future scalability.
- Updated the player scanning system so that active mute states are now captured and evaluated statically during scans, ensuring consistent moderation enforcement.
- Added an automated purge configuration for the `sw-scan` history, allowing cleanup to be defined based on a number of days.
- Modified the purge execution behavior so that expired data is no longer removed immediately upon reaching the configured day limit, but instead is processed after the next server restart.
- Removed the `default.temp` configuration key; temporary silence durations are now applied dynamically based on the defined mute context.

### Added
- Enhanced the `anti-domain` security logic, introducing stricter validation rules and adding a dedicated configuration file located at `moderation/security-filters.yml`.

### Fixed
- Fixed an issue where presets were not being displayed correctly when executing the `/sw-tempmute` command.
