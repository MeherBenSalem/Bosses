# Contributing to Remnant Bosses

Thank you for your interest in contributing!

## How to contribute

1. **Fork** the repository and create a feature branch from `main`.
2. Make your changes in the appropriate version workspace (`1.20.1/` or `1.21.1/`). Shared gameplay code goes in `common/`.
3. **Build** both workspaces to verify nothing is broken:
   ```
   ./gradlew buildAll
   ```
4. Open a **pull request** with a clear description of the change.

## Coding expectations

- Follow the existing code style and package structure.
- Keep loader modules thin — gameplay logic belongs in `common/`.
- Test your changes in-game on the relevant loader(s) before submitting.

## Reporting issues

Use GitHub Issues. Include your Minecraft version, mod loader, and steps to reproduce.

## License

Unless you explicitly state otherwise, any contribution you intentionally submit for inclusion in this project shall be licensed under the Apache License 2.0, without any additional terms or conditions.
