# Contributing to camouflage-kotlin

Thanks for helping improve the Kotlin implementation of Camouflage!

## Getting started
- Ensure you have a recent JDK (e.g., 17+) and Kotlin toolchain installed.
- Use Gradle to build and test.

## Development workflow
1. Fork and create a branch from `main`: `feat/<scope>-<short-desc>` or `fix/<scope>-<short-desc>`.
2. Implement your change.
3. Run formatting and static checks before committing:
   - Formatting: use the project’s configured formatter (e.g., ktfmt/ktlint if present) or IntelliJ default Kotlin style.
   - Build & tests: `./gradlew build test`
4. Update README/docs if behavior changes.
5. Open a Pull Request in the root repo targeting changes under `kotlin-lib/` and fill out the PR template.

## Testing
- Add/adjust unit tests to cover new or changed behavior.
- Ensure `./gradlew test` passes locally.

## Commit & PR guidelines
- Write clear commit messages and PR descriptions; link issues (e.g., "Fixes #123").
- Keep PRs focused and reasonably small.
- Include screenshots/logs for visible or behavioral changes when helpful.

## Code of Conduct
This project adheres to the Contributor Covenant.
See CODE_OF_CONDUCT.md. For sensitive reports, email contact@srctool.org.

## License
Contributions to `camouflage-kotlin` are made under the Apache 2.0 license found in LICENSE.

Thank you for contributing!