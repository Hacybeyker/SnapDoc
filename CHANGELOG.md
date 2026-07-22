# Changelog — ScaffoldingAndroidCompose

> Todos los cambios notables de este proyecto están documentados aquí.
> Formato basado en [Keep a Changelog](https://keepachangelog.com/es/1.1.0/).

---

## [Unreleased]

### ✨ Added
- Skill `code-reviewer`: revisión de código contra los estándares de `AGENTS.md` (arquitectura VSA + Clean + MVI, coding standards, testing, seguridad) con verificación automatizada del DoD y reporte por severidad.
- Scaffolding inicial para proyectos Android nativos con Jetpack Compose:
  - Arquitectura Vertical Slice + Clean + MVI con feature de ejemplo (`feature/home`) y tests (JUnit + Turbine + Fakes).
  - Hilt (DI por feature) y Navigation 3 (NavKeys type-safe).
  - Design tokens Material 3 (`core/ui/theme/`: Color, Type, Shape, Spacing).
  - Calidad de código: ktlint + detekt + Android Lint con tareas `codeQuality` y `formatAndAnalyze`.
  - Infraestructura de IA: `AGENTS.md` + `.agents/` (skills `android-best-practices`, `feature-implementation`, `git-commit`, `changelog-generator`, `skill-creator`, `skill-linker`) con symlinks multi-IDE.
  - `init-project.sh`: inicializador que renombra proyecto/package, limpia el ejemplo y configura git.
  - CI/CD: GitHub Actions (`ci.yml`, `release.yml`) + Dependabot.
  - Pre-commit hook de calidad (`scripts/setup-quality-hook.sh`).

[Unreleased]: https://github.com/hacybeyker/ScaffoldingAndroidCompose/commits/main
