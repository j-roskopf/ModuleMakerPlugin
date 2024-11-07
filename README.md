<div align="center">
  <img src="./assets/icon.svg" width="128px">
  <h1>Module Maker</h1>
</div>

<p align="center">
  <a href="https://opensource.org/license/mit/"><img alt="License" src="https://img.shields.io/badge/License-MIT-blue.svg"/></a>
  <a href="https://androidweekly.net/issues/issue-579"><img alt="Android Weekly" src="https://skydoves.github.io/badges/android-weekly.svg"/></a>
  <a href="https://us12.campaign-archive.com/?u=f39692e245b94f7fb693b6d82&id=fb7f5353b9"><img alt="Kotlin Weekly" src="https://skydoves.github.io/badges/kotlin-weekly.svg"/></a>
  <a href="https://github.com/j-roskopf/ModuleMakerPlugin/actions/workflows/release.yml"><img alt="Release Workflow" src="https://github.com/j-roskopf/ModuleMakerPlugin/actions/workflows/release.yml/badge.svg"/></a>
  <a href="https://plugins.jetbrains.com/plugin/21724"><img src="https://img.shields.io/jetbrains/plugin/v/21724.svg"/></a>
  <a href="https://plugins.jetbrains.com/plugin/21724"><img src="https://img.shields.io/jetbrains/plugin/d/21724.svg"/></a>
  <a href="https://hitsofcode.com/github/j-roskopf/ModuleMakerPlugin?branch=main"><img src="https://camo.githubusercontent.com/adc9708a2336e24e5a557d4b6d25c5bc21401eb8966a3c8eb34eba0871eb5a72/68747470733a2f2f686974736f66636f64652e636f6d2f6769746875622f6a2d726f736b6f70662f4d6f64756c654d616b6572506c7567696e3f6272616e63683d6d61696e"/></a>
</p><br>

<!-- Plugin description -->
This is a plugin that allows one to create modules without having to copy / paste / modify existing modules.

Creating both single modules and enhanced modules (representing the 3 module system outline [here](https://www.droidcon.com/2019/11/15/android-at-scale-square/))

Additional features include:

1. Specifying gradle template for modules to align with your project specific defaults.
  1. Allows for custom variables to be replaced with generated values
2. Aligning the gradle files to follow the module name
3. Generating both .gradle and .gradle.kts build files for a given module
<!-- Plugin description end -->

# Demo

https://www.youtube.com/watch?v=ZtXCxBuiQNk

## Building

Creating a release tag that follows `release/x.x.x` will create a Github release with the relevant artifacts.

## How To Use

- From under the `Tools` menu

  <kbd>Tools</kbd> > <kbd>Module Maker</kbd>

## Installation

<div align="center"><a href="https://plugins.jetbrains.com/plugin/21724-module-maker"><img src="assets/marketplace.png"/></a></div>

- Using IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "Module Maker"</kbd> >
  <kbd>Install Plugin</kbd>

- Manually:

  Download the [latest release](https://github.com/j-roskopf/ModuleMakerPlugin/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>
