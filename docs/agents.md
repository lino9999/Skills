# AGENTS.MD - AI Developer & Maintenance Guide for Skills Plugin

> **Purpose**: This guide provides any AI assistant or human developer with an immediate, token-efficient understanding of the **Skills** plugin architecture, lifecycle, data models, and extension points. Read this document before inspecting code files to minimize unnecessary context loading and token usage.

---

## 1. Project Overview & Identity
- **Plugin Name**: `Skills`
- **Author**: `Lino`
- **Target Platform**: Paper, Spigot, Purpur, Folia (supports Minecraft `1.21.11` through `26.3+`)
- **Language Level**: Java 21 LTS
- **Build System**: Gradle 9.x (wrapper provided: `gradlew.bat` / `./gradlew`)
- **Key Design Philosophy**:
  - 100% standalone plugin architecture with zero external core libraries.
  - High performance, asynchronous database I/O (SQLite with batch write transactions and YAML flatfile alternative).
  - Folia-safe execution (`SchedulerAdapter` wraps Paper/Folia region and entity schedulers).
  - Consolidated YAML configuration (`config.yml`, `messages.yml`, `attributes.yml`, `talents.yml`, `skills.yml`).
  - Zero code comments (self-documenting, clean, idiomatic Java). All messages and UI strings are in English.

---

## 2. Token-Saving Guide for AI Agents
1. **Never read binary files or build outputs**: Do not inspect `.jar`, `.class`, `.zip`, `.gradle/`, or `build/`.
2. **Targeted Inspections Only**:
   - To add a new skill trigger/event: Inspect `com.lino.skills.events.*`.
   - To alter GUI layouts: Inspect `com.lino.skills.ui.*` and `config.yml`.
   - To modify attribute calculation: Inspect `com.lino.skills.service.AttributeService`.
   - To add commands or subcommands: Inspect `com.lino.skills.command.*`.
   - To update database tables: Inspect `com.lino.skills.persistence.SqliteProfileStore`.
   - To add placeholders: Inspect `com.lino.skills.integration.PapiExpansion`.

---

## 3. Directory & Package Map

```
Skills/
├── build.gradle
├── settings.gradle
├── docs/
│   ├── agents.md
│   ├── spigot_bbcode.txt
│   └── spigot_docs_bbcode.txt
├── src/main/
│   ├── java/com/lino/skills/
│   │   ├── SkillsPlugin.java        # Plugin entrypoint and service container
│   │   ├── command/                 # PlayerCommandController, StatsCommandController, AdminCommandController
│   │   ├── domain/                  # SkillDefinition, AttributeDefinition, TalentDefinition, TierGrant, PlayerProfile, RankEntry
│   │   ├── events/                  # HarvestHandler, BattleHandler, SlayerHandler, AnglingHandler, EnchantmentHandler, BreweryHandler, RoamHandler, DurabilityHandler, FloatingTagHandler, SessionHandler
│   │   ├── integration/             # PapiExpansion (PlaceholderAPI)
│   │   ├── persistence/             # ProfileRepository, ProfileStore, SqliteProfileStore, YamlProfileStore
│   │   ├── platform/                # SchedulerAdapter, ColorFormatter, FormulaParser, IconFactory
│   │   ├── registry/                # ConfigRegistry (YAML loader)
│   │   ├── service/                 # AttributeService, ProgressionService, ManaService, RankingService, HudService
│   │   └── ui/                      # GuiView, GuiRouter, OverviewGui, AttributesGui, RoadmapGui
│   └── resources/
│       ├── plugin.yml               # Plugin manifest, commands, permissions
│       ├── config.yml               # Storage, HUD bar, combat tags, notifications, menu layouts
│       ├── messages.yml             # English locale strings & menu titles
│       ├── attributes.yml           # 9 RPG Attributes definitions
│       ├── talents.yml              # 26 Passive Talents definitions
│       └── skills.yml               # 9 Skills, XP curves, grants, and block XP tables
```
