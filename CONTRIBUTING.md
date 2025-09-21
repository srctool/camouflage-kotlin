# Contributing to camouflage-kotlin

Thanks for helping improve the Kotlin implementation of Camouflage!

## Getting started
- Install a recent JDK (17+) and the Kotlin toolchain.
- Use Gradle for builds and tests.
- Recommended IDE: IntelliJ IDEA or Android Studio with Kotlin plugin.

## Development workflow
1. Fork the repo and create a branch from `main`.
   - Suggested: `feat/<scope>-<short-desc>` or `fix/<scope>-<short-desc>`
2. Implement your change with small, focused commits.
3. Before committing, run formatting and checks:
   - Format: use the project’s formatter (ktfmt/ktlint if configured) or IntelliJ default Kotlin style.
   - Build & tests: `./gradlew build test`
4. Update README/docs if behavior changes.
5. Open a Pull Request in the root repo targeting files under `kotlin-lib/` and fill out the PR template.

### PR title format (Conventional Commits)
Format your PR title as:

```
<type>(<scope>): <short description>
```

- type: one of `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`
- scope: the affected module/package (e.g., `kotlin-lib`, `core`, `logging`)
- short description: concise summary

Examples:
- feat(kotlin-lib): add redact() helper for masked logging
- fix(kotlin-lib): prevent NPE in TokenParser when input is blank

GitKraken tip: GitKraken uses the first line of the commit message as the PR title. You can copy the PR title format directly when committing.

## Testing
- Add/adjust unit tests to cover new or changed behavior.
- Ensure `./gradlew test` passes locally.
- Prefer deterministic tests; avoid relying on clock/time or networking unless mocked.

## Commit & PR guidelines
- Write clear commit messages and PR descriptions; link issues (e.g., "Fixes #123").
- Keep PRs focused and reasonably small.
- Include screenshots/logs for visible or behavioral changes when helpful.

## Code of Conduct
This project adheres to the Contributor Covenant.
See `CODE_OF_CONDUCT.md`. For sensitive reports, email contact@srctool.org.

## License
Contributions to `camouflage-kotlin` are made under the Apache 2.0 license found in `LICENSE`.

Thank you for contributing!