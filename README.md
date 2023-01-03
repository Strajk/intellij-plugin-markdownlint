# intellij-plugin-markdownlint

<!--
![Build](https://github.com/Strajk/intellij-plugin-markdownlint/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)
-->

🙈 **NOTE: This is my first ever Kotlin project, so please be gentle.**

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


## FAQ

### It's slooow!

I know, but I don't know how to make it faster. I'm open to suggestions. This is my first ever Kotlin project so I probably did something wrong.

### Getting `java.io.IOException: Cannot run program "node"`

Your IDE does not have `node` in its `PATH`. 
If you have node installed and it works elsewhere (e.g. in the terminal), 
the probable cause is that your "normal" `PATH` is not the same as the `PATH` used by IntelliJ.
That's usually caused by a custom `.bashrc`/`.zshrc`/`...` that adjust your `PATH` for your terminal, but not for IntelliJ IDEs.
**Hotfix** is to launch your IDE from the terminal, so that it inherits your `PATH` from the terminal.
```
open -a "WebStorm"` on macOS
`webstorm` on Linux
`webstorm.exe` on Windows
```
Read more on [StackOverflow](https://stackoverflow.com/questions/15201763/intellij-does-not-recognize-path-variable).

Proper fix would be to not use `node` from the plugin, but I simply don't know how to do that yet ¯\_(ツ)_/¯

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
