# intellij-plugin-markdownlint

<!--
![Build](https://github.com/Strajk/intellij-plugin-markdownlint/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)
-->

😱 **BEWARE: This is a work in progress. It's not ready for use yet, but I wanted to publish it to get feedback.**

<!-- Plugin description -->
This plugin integrates [markdownlint](https://github.com/DavidAnson/markdownlint) into IntelliJ IDEs.
<!-- Plugin description end -->

![Screenshot](screenshot.png)

## Installation

- ~~Using IDE built-in plugin system~~: 🚫 NOT YET PUBLISHED 🚫
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "intellij-plugin-markdownlint"</kbd> >
  <kbd>Install Plugin</kbd>
  
- Manually:

  Download the [latest release](https://github.com/Strajk/intellij-plugin-markdownlint/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>


## Feature parity with "official" [VSCode extension](https://github.com/DavidAnson/vscode-markdownlint)

- [ ] [Fixing](https://github.com/DavidAnson/vscode-markdownlint#fix)
- [ ] [Config detection](https://github.com/DavidAnson/vscode-markdownlint#markdownlintconfig)
- [ ] [Focus/Zen mode](https://github.com/DavidAnson/vscode-markdownlint#markdownlintfocusmode)
- [ ] [Snippets](https://github.com/DavidAnson/vscode-markdownlint#snippets)

## Issues/Todo

- [ ] Add tests
- [ ] Intention to Suppress
- [ ] Intention to Fix

## Development

- Plugin should be able to "Auto-Reload", but does not work for me. I have to re-run "Run plugin" configuration every time I make a change.
- Debugging seems to work fine, at least for the "Run Plugin" configuration.
  - Does not work for me for "Run Tests" configuration. 
