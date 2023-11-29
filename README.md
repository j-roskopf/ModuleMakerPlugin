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
