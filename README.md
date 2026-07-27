Sleep-Debt-Sim
=================

A small, student-facing Java 21 Swing desktop application that models sleep debt from calendar dates and hours slept. This repository is intended for learning, experimentation, and small collaborative development. See StateOfTheCodebase.md for a deeper investigation and prioritized improvement plan.

Quick links
-----------
- Entry point: org.example.App
- Main UI: org.example.ui.SimulatorApp
- Core calculation: org.example.util.CalculateGraph
- Investigation notes: StateOfTheCodebase.md

Prerequisites
-------------
- JDK 21 (Temurin or other distribution)
- Gradle wrapper (included: ./gradlew on Unix, gradlew.bat on Windows)

Build and test
--------------
Run tests and build from the repository root:

- Unix/macOS:
  - ./gradlew clean test
  - ./gradlew :app:run

- Windows (PowerShell / cmd):
  - .\gradlew.bat clean test
  - .\gradlew.bat :app:run

Continuous Integration
----------------------
A GitHub Actions workflow (.github/workflows/ci.yml) runs on push and pull_request to main/master and executes:
- ./gradlew spotlessCheck
- ./gradlew clean test

Formatting and editor config
----------------------------
This project enforces consistent formatting:
- Spotless with Google Java Format (configured in app/build.gradle.kts)
- .editorconfig at the repo root for basic editor settings (LF, UTF-8, 4-space indent)

Local formatting commands:
- Apply formatting (auto-fix): ./gradlew spotlessApply
- Check formatting (CI uses this): ./gradlew spotlessCheck

Git hooks (recommended)
-----------------------
A pre-commit hook is provided under .githooks/pre-commit and a small installer script copies it into .git/hooks.

To enable the hook after cloning (Windows PowerShell):

  powershell -NoProfile -ExecutionPolicy Bypass -File ./scripts/install-git-hooks.ps1

On Unix/macOS (manual install):

  cp .githooks/pre-commit .git/hooks/pre-commit
  chmod +x .git/hooks/pre-commit

What the hook does:
- Runs Spotless (spotlessApply) to auto-format staged Java files
- Re-stages any formatting fixes (git add -A)
- Optionally runs tests if RUN_TESTS is set (not recommended by default)

This keeps formatting consistent and prevents CI failures for style differences.

Development workflow and conventions
------------------------------------
To make collaboration (and multi-agent work) safe and low-friction, follow these guidelines:

Branching
- Create short-lived branches named with this convention: feat/<who>/<short-description>
  e.g. feat/agentA/extract-night-repo

Commit practices
- Make small, focused commits with one logical change per commit.
- Include tests for behavioral changes, especially for business logic (CalculateGraph, CSV parsing, Night merging).

Pull Requests
- Open a draft PR early and iterate.
- Ensure CI passes (spotlessCheck + tests) before requesting review.

Minimal PR checklist (copy into PR description):
- [ ] Branch name follows convention.
- [ ] Tests added or updated where behavior changed.
- [ ] ./gradlew spotlessCheck passes locally.
- [ ] ./gradlew clean test passes locally.
- [ ] StateOfTheCodebase.md updated if conventions or architecture changed.

High-leverage tasks to prepare the repo for multi-agent work
-----------------------------------------------------------
The project investigation recommends the following priorities (already underway):
1. Add CI that runs tests and formatting (done).
2. Add/enable Spotless formatting + pre-commit hook (done).
3. Extract duplicated merge/upsert logic into a testable NightRepository.
4. Add focused tests for CalculateGraph.computeValues to validate the debt/decay math.
5. Replace naive CSV parsing with a robust CSV parser and add error-path tests.
6. Migrate the Night model to use LocalDate internally and confine Excel-serial handling to the CSV boundary.

Where to find investigative notes
---------------------------------
See StateOfTheCodebase.md (generated during the repo audit) for detailed findings, file-level notes, and suggested next steps.

Getting help
------------
If something in the build or hooks doesn't work on your machine, open an issue or contact the repo maintainer. Include your OS and the exact error output.

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
