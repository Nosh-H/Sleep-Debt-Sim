Sleep-Debt-Sim — Codebase Investigation Report
No README.md exists anywhere in the repo (confirmed via search) — the closest thing to onboarding docs is .github/instructions/default.instructions.md, an AI-assistant instruction file that doubles as informal project documentation.

1. Tech Stack & Structure
Language/Runtime: Java 21 (app/build.gradle.kts:38-42 pins JavaLanguageVersion.of(21) via toolchain).
Build tool: Gradle (Kotlin DSL), using the application plugin. Files: c:\Users\nhhas\Documents\Code-Repositories\Sleep-Debt-Sim\app\build.gradle.kts, settings.gradle.kts, gradle/libs.versions.toml, gradlew/gradlew.bat.
App type: Desktop GUI app (Java Swing), single-module Gradle project named app. Entry point declared as org.example.App (app/build.gradle.kts:46).
Key libraries (declared in app/build.gradle.kts:18-35):
com.google.guava:guava:33.6.0-jre — only used for Doubles.tryParse input validation.
org.knowm.xchart:xchart:3.8.5 — charting/plotting library (Swing-based).
com.github.lgooddatepicker:LGoodDatePicker:11.2.1 — calendar date-picker widget.
org.junit.jupiter (JUnit 6.0.1 per gradle/libs.versions.toml) — testing.
Note: xchart and LGoodDatePicker are declared as raw string dependencies directly in build.gradle.kts rather than being added to the gradle/libs.versions.toml version catalog (which only lists guava and junit-jupiter) — a minor inconsistency in the project's own convention.
Folder structure:

app/
  src/main/java/org/example/
    App.java                     — entry point (Swing bootstrap)
    Constants.java                — shared tuning constants
    sim/Simulation.java           — dead/demo code, explicitly unused
    ui/SimulatorApp.java          — the entire UI (window, controls, chart, event handlers)
    util/CalculateGraph.java      — core sleep-debt math
    util/CsvNightExporter.java    — CSV write
    util/CsvNightLoader.java      — CSV read/parse
    util/ExcelDateConverter.java  — Excel-serial <-> LocalDate conversion
    util/Night.java               — record (date serial, hours slept)
    util/ResultWrapper.java       — record (x/y coordinate lists)
  src/test/java/org/example/...   — mirrors main package structure
template-data.csv                 — sample/demo CSV data at repo root
.github/instructions/default.instructions.md — AI-agent-facing project conventions doc
Generated build artifacts (app/build/, .gradle/, root build/) are present in the working tree but a recent commit (7e6acea "Stop tracking generated app/bin/ directory") shows an effort to keep these out of git — worth verifying .gitignore fully excludes app/build/ and .gradle/ since they were found on disk (git status shows tree is clean, so likely already ignored).
2. Data Handling
There is no database — everything is in-memory plus optional CSV import/export.

In-memory model: SimulatorApp holds an ArrayList<Night> nights field (SimulatorApp.java:46) synchronized manually with synchronized (nights) { ... } blocks scattered through the class (lines 143, 187-190, 214-232, 238-240). This is the entire "data layer" — there's no separate repository/service class; state lives directly inside the UI controller.
Night record (util/Night.java:3): public record Night(int excelDateSerial, double hoursSlept) {} — minimal, immutable, good use of records.
ResultWrapper record (util/ResultWrapper.java): wraps parallel ArrayList<Integer> x / ArrayList<Double> y lists for chart coordinates — a data-transfer object that could arguably be two separate typed points (List<Point>) rather than parallel arrays, but is fine for the current scale.
CSV import (util/CsvNightLoader.java):
load(Path) / load(Reader) — reads line by line, splits on comma, tolerates a header row via a heuristic looksLikeHeader (CsvNightLoader.java:76-80) that inspects whether column text contains "date"/"serial"/"hour".
Parses dates via ExcelDateConverter.parseDateToken supporting Excel serial ints, ISO yyyy-MM-dd, and M/d/uuuu/M/d/uu.
Performs sorting and de-duplication at load time (CsvNightLoader.java:54-67): sorts by excelDateSerial, then dedupes consecutive same-date entries keeping the later one.
Naive CSV parsing via String.split(",", -1) — no support for quoted fields, embedded commas, or CRLF/locale decimal separators; fragile for real-world CSV exports (e.g., from Excel with ; locales or quoted text).
CSV export (util/CsvNightExporter.java): writes Date,Hours,Sleep Deficit,Sleep Debt with values recomputed via CalculateGraph. Reasonably clean, small, testable static utility.
Date representation: dates are stored as Excel serial integers rather than LocalDate/Instant, with conversion logic isolated in util/ExcelDateConverter.java. This is an unusual choice for a Java app (a holdover from spreadsheet-oriented import) and adds a layer of indirection/bug surface (e.g., the Feb-29-1900 leap bug workaround at lines 29-36, and the serial < 1 / serial == 60 guard clauses) that a plain LocalDate field on Night would avoid entirely.
Sample data: template-data.csv at repo root (70 lines) is example/manual test data with two duplicate dates (2026-06-04 appears twice, lines 14-15) — presumably intentional to exercise the dedup logic manually.
Merging imported CSV nights with existing in-memory nights is done via a hand-rolled linear insertion-sort loop duplicated almost verbatim in two places (see Code Quality section below).
3. UI / Graphing
UI framework: Java Swing, built imperatively and monolithically inside SimulatorApp.start() (app/src/main/java/org/example/ui/SimulatorApp.java), which is ~170 lines long and constructs the entire window: date picker, hours text field, four buttons (Add Date, Upload CSV, Download CSV, Switch to Dates), a JList for entries, and the chart panel — plus all their addActionListener handlers — in one method (lines 50-211).
Charting library: org.knowm.xchart (XYChart, XYChartBuilder, XChartPanel, QuickChart, SwingWrapper).
Recent graphing-related work (matches the requested commit topics):
c815213 "Sorting and deduplication by date serial" — implemented in CsvNightLoader.load (dedup) and duplicated manually in SimulatorApp.addNightAndRefresh/the upload handler (insertion-sort-with-replace loops at SimulatorApp.java:143-162 and 213-233).
94c68d8 "X axis: switch between days/dates" — implemented via showAbsoluteDates boolean field (SimulatorApp.java:47) and the toggleAxisBtn handler (lines 200-204) plus refreshChartAndList (lines 236-275), which removes and re-adds the XChart series every refresh because "XChart's updateXYSeries does not support switching between numeric and Date x-values on an existing series" (comment at lines 251-253). This is a reasonable workaround but means the chart is rebuilt from scratch on every state change rather than incrementally updated — fine at this data scale, but a red flag if data volume grows.
Chart data flow: CalculateGraph.computeValues(nights) (util/CalculateGraph.java:15-47) does the actual sleep-debt math (O(n²) nested loop, intentionally simple per project instructions at line 35 of the instructions doc) and returns a ResultWrapper. SimulatorApp.refreshChartAndList then converts x-values to java.sql.Date when showAbsoluteDates is true (line 257-262) or leaves them as day-offset integers otherwise.
Quality observations specific to the graphing code:
refreshChartAndList mixes UI mutation (chart series add/remove, styler config, list model population) with a call into pure business logic (CalculateGraph.computeValues) and formatting (formatNightForDisplay) all in one 40-line method — no separation between "compute what to plot" and "render it."
Chart styling calls (setDatePattern, setXAxisLabelRotation, setXAxisTitle) are re-issued on every refresh regardless of whether the axis mode changed, which is harmless but slightly wasteful/repetitive.
launch(ArrayList<Integer> days, ArrayList<Double> debt) (SimulatorApp.java:282-285) is leftover/dead code labeled "Former temporary start function" — never called from App.main (which now calls .start()), and it re-uses QuickChart/SwingWrapper instead of the app's real chart panel. This should probably be deleted.
org.example.sim.Simulation (sim/Simulation.java) is entirely dead/demo code — a sine-wave ticker with a Listener interface, explicitly annotated // Currently not using (line 9) and called out in the instructions file as "demo or unused scaffolding" that should not be removed without explicit intent. It's unused by anything in App/SimulatorApp.
4. Testing
Framework: JUnit Jupiter (JUnit 5-style API, version 6.0.1 per gradle/libs.versions.toml), run via Gradle's useJUnitPlatform() (app/build.gradle.kts:49-52).
Test files (app/src/test/java/org/example/...):
AppTest.java — a single generated placeholder test (appHasAGreeting) testing the Gradle-init leftover getGreeting() method, which is explicitly not real app behavior (per instructions doc line 68). Contributes zero real coverage.
util/CalculateGraphTest.java — one test, verifying that computeValues sorts unsorted input and produces correct day-offsets. Does not test the actual sleep-debt magnitude/decay math (sleepDebt accumulation, DEFICIT_DECAY_POWER, DEFICIT_MULTIPLIER), nor computeSleepDeficits, nor edge cases like empty/null input (though the code path exists at CalculateGraph.java:19-23).
util/CsvNightExporterTest.java — 2 tests: correct sorted output with deficit/debt columns, and empty-list handling. Reasonably solid for what it covers.
util/CsvNightLoaderTest.java — 3 tests: header + mixed Excel/ISO date parsing, round-trip serial conversion, and sort+dedupe-with-duplicates behavior. Good coverage of the loader's primary contract, but no tests for malformed rows (missing column, unparsable date/hours) even though CsvNightLoader.load throws IOException in those cases (lines 40, 49) — this error path is completely untested.
util/ExcelDateConverterTest.java — 1 parameterized-style test looping serials 58-63, checking round-trip and the serial-60 leap bug guard. Does not test parseDateToken at all (the multi-format string parser with 3 fallback formatters — ExcelDateConverter.java:40-67), nor toDisplayString.
Zero test coverage for:
org.example.ui.SimulatorApp — the largest and most complex class (~285 lines), containing all button-click business logic (insertion/merge logic, axis toggling, file dialogs), entirely untested. This is understandable since it's Swing UI code (hard to unit test without a headless harness), but the non-UI logic embedded in it (e.g., the insert-or-replace-sorted algorithm duplicated twice) has zero coverage and would benefit from being extracted into a testable class.
org.example.Constants — trivial, doesn't need tests.
org.example.sim.Simulation — dead code, untested (understandably, since it's unused).
org.example.App — only the placeholder greeting is tested; the real main method is untested (though hard to test Swing bootstrap).
CalculateGraph.computeSleepDeficits — package-private, has no direct test (only indirectly exercised via CsvNightExporterTest).
CalculateGraph.millisecondsToDays / daysToMilliseconds (CalculateGraph.java:61-67) — appear completely unused anywhere in the codebase (dead utility methods) and are untested.
Test-to-source ratio: 5 test files vs. 9 main source files, but only ~10 actual test methods total, concentrated on CSV/date utilities. The core financial/debt math (the actual "simulation" the app is named for) has exactly one shallow test.
CI: No .github/workflows/ directory was found — no evidence of automated CI running these tests on push/PR (only the .github/instructions/ folder exists, which is Copilot/AI guidance, not a workflow).
5. Code Quality Issues
Concrete, cited issues:

Duplicated insertion/merge logic — the "insert-or-replace, keep sorted by serial" algorithm appears twice, nearly identical:

SimulatorApp.java:143-162 (inside the upload-CSV handler, looping over importedNights)
SimulatorApp.java:213-233 (addNightAndRefresh, for a single new night) Both do the same linear scan + insert-at-index dance. This should be a single method (e.g., upsertNight(Night) reused by both call sites, or by a dedicated NightRepository/NightCollection class).
God-method UI class — SimulatorApp.start() spans SimulatorApp.java:50-211 (161 lines) and is responsible for: window construction, layout, widget creation, per-button event-handler bodies containing file I/O and data-merging logic, and initial chart setup. There is no separation between "build UI," "handle events," and "manage domain state" — all three concerns are interleaved in one method. This makes it hard to test, hard to extend (e.g., adding an "edit entry" or "delete entry" feature means touching this same sprawling method), and a magnet for merge conflicts.

UI/business logic mixed together — file-dialog handling, CSV parsing error display (JOptionPane), and the sorted-merge algorithm are all inline inside ActionListener lambdas (e.g., uploadBtn.addActionListener at lines 128-168). None of this merge/validation logic can be unit tested without instantiating Swing components.

Synchronization is inconsistent/likely unnecessary — nights is guarded with synchronized (nights) { ... } blocks throughout SimulatorApp (e.g., lines 143, 187, 214, 238), implying the author expected multi-threaded access. But all callers are Swing ActionListeners, which by Swing convention run exclusively on the EDT — so there should be no concurrent access in practice, making the synchronization either dead defensive code or a sign that some code path does touch nights off the EDT (which would itself be a Swing-threading bug per the very rule App.java's comment praises at lines 15-24). Either the synchronization is unnecessary complexity, or there's an undocumented threading concern that isn't explained anywhere.

Dead code retained deliberately but never cleaned up:

org.example.sim.Simulation (entire file, sim/Simulation.java) — unused sine-wave demo.
SimulatorApp.launch(...) (SimulatorApp.java:282-285) — "Former temporary start function," never invoked.
CalculateGraph.millisecondsToDays / daysToMilliseconds (CalculateGraph.java:61-67) — no callers found anywhere in app/src.
Constants.naturalFatigueVerticalTranslation (Constants.java:9) — declared but "not yet wired into the graph" per the instructions doc (line 39); dead configuration. These are individually small, but collectively they add noise for anyone new to the codebase trying to understand what's actually load-bearing.
Magic numbers / unexplained tuning constants — Constants.DEFICIT_DECAY_POWER = 1.25 and DEFICIT_MULTIPLIER = 1 (Constants.java:11-12) drive the entire debt-decay model in CalculateGraph.computeValues (CalculateGraph.java:38: sleepDeficits.get(j) / Math.pow((i + 1 - j), Constants.DEFICIT_DECAY_POWER) * Constants.DEFICIT_MULTIPLIER) with no comment justifying why 1.25 was chosen, no unit test asserting the formula's behavior beyond ordering, and no citation of a sleep-science source. Given the app's entire purpose is this calculation, this formula being unexplained and only indirectly tested is the single biggest "hidden risk" in the codebase.

Constants uses public mutable static fields — optimalHours and naturalFatigueVerticalTranslation are public static double (non-final) fields (Constants.java:6,9), i.e. global mutable state accessible from anywhere with no encapsulation, validation, or change-notification. The TODO comment at line 5 ("Add lombok setters to allow user to configure values") acknowledges this is a stopgap. Any future "let the user configure optimal sleep hours" feature will need to route through the UI to update chart state anyway, so a settings object with proper encapsulation would be more robust than public static mutation.

Naive CSV parsing — CsvNightLoader.load uses trimmedLine.split(",", -1) (CsvNightLoader.java:38) rather than an RFC 4180-aware CSV parser. It will break on quoted fields, embedded commas, or extra whitespace variants. No CSV library dependency is used despite Guava already being a dependency (or a small library like opencsv/commons-csv could easily be added).

Error handling is dialog-only, not logged — All I/O exceptions in SimulatorApp are caught and shown via JOptionPane.showMessageDialog (e.g., SimulatorApp.java:165-167, 195-197) with no logging (no Logger/java.util.logging/slf4j usage anywhere in the codebase). If something goes wrong in production/testing, there's no trace beyond what the user sees and possibly loses.

ExcelDateConverter — internal date representation choice. Storing dates as Excel serial ints inside Night (rather than LocalDate) forces every consumer (CalculateGraph, SimulatorApp, both CSV classes) to convert back and forth via ExcelDateConverter, and introduces edge-case handling for Excel's infamous 1900 leap-year bug (ExcelDateConverter.java:29-36) purely to support one CSV input format. A cleaner design would store LocalDate internally and only touch Excel-serial conversion at the CSV parse/format boundary.

No .editorconfig/formatter/linter config found — no Checkstyle, Spotless, or .editorconfig in the repo; style consistency depends entirely on manual review. Combined with no CI workflow, nothing currently enforces build success, test passing, or style before merge.

Version catalog inconsistency — xchart and LGoodDatePicker dependencies are hardcoded as string literals directly in app/build.gradle.kts:28,31 instead of being declared in gradle/libs.versions.toml alongside guava/junit-jupiter, which is inconsistent with the version-catalog pattern the project otherwise uses.

6. Git History Hints
Full history (16 commits total, git log --oneline -30):


94c68d8 2026-06-25 X axis: switch between days/dates
c815213 2026-06-22 Sorting and deduplication by date serial
7e6acea 2026-06-22 Stop tracking generated app/bin/ directory
dae49d1 2026-06-21 Merge branch 'codex/add-csv-upload-button'
6f5cf24 2026-06-21 Update .gitignore
384f3c9 2026-06-21 CodeRabbit feedback (fixes + unit tests)
fbf49a9 2026-06-21 Add Download CSV button
749f5b2 2026-06-21 Adjust example data, remove unused imports in App.java
12e6c03 2026-06-20 Add CSV import for sleep debt simulation
6908705 2026-06-20 Merge branch 'main' of .../Sleep-Debt-Sim
97aaf86 2026-06-20 Input validation
9205ff9 2026-05-20 Merge branch 'main' of .../Sleep-Debt-Sim
ad6a2ba 2026-05-20 git ignore local JDK (on macbook)
0946d5f 2026-05-19 Fix x axis point calculation (GUI input)
e30aab2 2026-05-19 Allow user input through GUI first pass
1ce7fea 2026-05-17 Add License
3ccd8e6 2026-05-17 Initial commit - Swing-based simulator, reads from csv files
Observations:

The project is very young (first commit 2026-05-17, most recent 2026-06-25 — about 6 weeks of active history), and is clearly a learning/incremental project (per the AI instructions doc's framing as "student-facing").
Development has moved through clear phases: (1) initial CSV-driven Swing simulator, (2) GUI date/hours input added, (3) CSV import/export added (with an automated review tool, "CodeRabbit," providing feedback that was incorporated in 384f3c9), (4) sorting/dedup correctness fixes, (5) most recently, date-vs-day-offset X-axis toggling.
The CSV upload/import area (12e6c03, fbf49a9, 384f3c9, dae49d1, c815213) has seen the most churn and the most external review (CodeRabbit feedback commit) — this is the area most likely to still have edge cases, consistent with the naive split(",") parsing and duplicated merge logic found above.
The X-axis/graphing logic (0946d5f, 94c68d8) has been touched twice for bug fixes/features, suggesting the chart's coordinate/axis handling is an area that has needed rework and could benefit from tests (currently has none for the axis-toggle behavior).
No commits touch Simulation.java after its introduction — confirms it's genuinely abandoned scaffolding.
No .github/workflows/ (CI) commits exist at all — testing has been manual/local via CodeRabbit's automated PR review rather than any in-repo CI pipeline.
Summary of Highest-Value Improvement Targets
Extract the sorted-merge/upsert logic out of SimulatorApp into a testable, non-UI class (e.g., NightRepository), eliminating the duplication at SimulatorApp.java:143-162 and 213-233.
Add real unit tests for the sleep-debt formula in CalculateGraph.computeValues (currently only ordering is tested, not the actual decay math) and for CsvNightLoader's malformed-input error paths.
Break up SimulatorApp.start() into smaller methods/classes (widget builder, event handlers, chart controller) to reduce the single 161-line method and improve testability.
Replace Excel-serial-based date storage with LocalDate throughout Night/CalculateGraph, confining Excel-serial conversion strictly to the CSV I/O boundary.
Remove or clearly quarantine dead code (Simulation.java, SimulatorApp.launch, unused CalculateGraph millisecond helpers, unused naturalFatigueVerticalTranslation).
Add a CI workflow (GitHub Actions) running ./gradlew test on push/PR, since none currently exists.
Replace naive split(",") CSV parsing with a proper CSV parser or explicit quoting rules, and add tests for malformed CSV rows.
Document/justify the DEFICIT_DECAY_POWER/DEFICIT_MULTIPLIER constants (or make them user-configurable per the existing TODO), since they're the core "simulation" the app is named for yet are unexplained magic numbers.
Here's a prioritized improvement list based on a full read of the codebase (Java 21 Swing app using XChart for graphing, CSV-based storage — no database).

Architecture / Data Management
Extract data logic out of the UI class. SimulatorApp (285 lines) owns the nights list directly and has the exact same "insert-or-replace, keep sorted" merge algorithm duplicated in two places (SimulatorApp.java:143-162 for CSV import, :213-233 for single-entry add). Pull this into a NightRepository/NightCollection class with one upsert(Night) method — fixes the duplication and makes the merge logic unit-testable independent of Swing.
Store dates as LocalDate, not Excel serial ints. Night currently stores excelDateSerial, forcing every consumer (CalculateGraph, SimulatorApp, both CSV classes) to convert back and forth via ExcelDateConverter, including a manual 1900-leap-year bug workaround. Confine Excel-serial handling strictly to the CSV import/export boundary.
Replace naive CSV parsing. CsvNightLoader splits on raw commas (split(",", -1)), so quoted fields, embedded commas, or Excel-locale exports will break it silently or throw unhelpfully. Swap in a small CSV library (Guava is already a dependency, so opencsv/commons-csv is a low-cost add) and add tests for malformed rows — currently the IOException error paths are completely untested.
Replace public mutable static config. Constants.optimalHours and naturalFatigueVerticalTranslation are non-final public static fields (global mutable state, acknowledged TODO in the code itself). Wrap in a small SimulationSettings object if/when these become user-configurable, rather than mutating statics from the UI.
Add logging. All I/O errors are swallowed into JOptionPane dialogs with no Logger anywhere in the codebase — if something fails during testing/use, there's no trace beyond what the user saw once.
UI
Break up SimulatorApp.start() (161 lines: window + layout + widgets + all button handlers + chart bootstrap in one method). Split into a widget-builder, an event-handler set, and a chart controller — this is also what's blocking meaningful UI-adjacent testing.
Add edit/delete for individual entries. Right now the only way to fix a bad manual entry is presumably re-adding it; there's no delete/edit action on the JList of nights.
Chart rebuild is full teardown/rebuild on every refresh (series removed and re-added each time because XChart won't switch numeric↔Date on an existing series) — fine at current scale, but worth a comment flagging it as a scaling concern, or worth checking if XChart's updateXYSeries overload set has improved in a newer release.
User-configurable "optimal hours." The Constants.optimalHours value that presumably drives the debt calc has a TODO for a settings UI — this would also let you validate item 4 above.
Database / Data Cleanup
There's no database, only CSV + in-memory state, so "cleanup" here means: dedupe/sort logic already exists (c815213) but is duplicated — see #1. Also worth having CSV export round-trip-verified against import (i.e., export then re-import should be lossless) — no test currently checks that round trip.
template-data.csv at repo root intentionally contains a duplicate date (lines 14-15) to exercise dedup manually — consider moving this into src/test/resources and referencing it from an automated test instead of relying on manual exercising.
Testing (currently ~10 test methods across 5 files, weighted toward CSV/date utilities)
Test the actual sleep-debt formula. CalculateGraphTest only checks sort order — the real decay math (DEFICIT_DECAY_POWER = 1.25, DEFICIT_MULTIPLIER) that the entire app is named for has no assertions on correctness, and the constants themselves are unexplained magic numbers with no cited rationale.
Test ExcelDateConverter.parseDateToken — the multi-format fallback parser (Excel serial / ISO / M/d/uuuu / M/d/uu) is completely untested; only round-trip serial↔date conversion is covered.
Test malformed CSV input in CsvNightLoader (missing column, unparsable date, unparsable hours) — all three throw IOException but none are exercised.
Delete dead code so coverage numbers mean something: org.example.sim.Simulation (unused demo class), SimulatorApp.launch(...) ("former temporary start function," never called), CalculateGraph.millisecondsToDays/daysToMilliseconds (no callers anywhere). These inflate the surface area and confuse "what actually needs tests."
Add a CI workflow. No .github/workflows/ exists at all — nothing currently runs ./gradlew test automatically on push/PR; test coverage improvements have no enforcement mechanism yet.
Quick wins (low effort, some cleanup value)
Move xchart and LGoodDatePicker into gradle/libs.versions.toml for consistency with how guava/junit-jupiter are declared.
Add an .editorconfig since there's currently no formatting/linting enforcement (no Checkstyle/Spotless either).
If you want, I can start on any of these — the highest-leverage first steps would likely be #1 (extract merge logic) and #12 (test the debt formula), since they unblock most of the rest safely.