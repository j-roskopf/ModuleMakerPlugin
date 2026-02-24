package com.joetr.modulemaker

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.CommonContainerFixture
import com.intellij.remoterobot.fixtures.ComponentFixture
import com.intellij.remoterobot.fixtures.JButtonFixture
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.stepsProcessing.step
import com.intellij.remoterobot.utils.keyboard
import com.intellij.remoterobot.utils.waitFor
import org.junit.Test
import java.awt.event.KeyEvent
import java.io.File
import java.time.Duration

/**
 * UI test that verifies the Module Maker plugin opens and can create a module.
 *
 * Prerequisites: the IDE must be running with the robot server plugin on port 8082.
 * Start it with: ./run-ui-tests.sh  (or two terminals: runIdeForUiTests then uiTest)
 *
 * Compose interaction:
 * Remote-robot's keyboard/mouse APIs don't reach the Skia canvas inside
 * ComposePanel. We dispatch Swing MouseEvent/KeyEvent directly to the
 * compose panel component via callJs + component.dispatchEvent(). This avoids
 * java.awt.Robot (which requires macOS accessibility permissions) and works because
 * ComposePanel forwards dispatched Swing events to the compose layer.
 */
class ModuleMakerUiTest {

    private val robotPort = System.getProperty("robot-server.port", "8082")
    private val remoteRobot = RemoteRobot("http://127.0.0.1:$robotPort")

    @Test
    fun `opens via Find Action and creates repository module`() {
        with(remoteRobot) {
            step("Wait for IDE to be ready and dismiss blocking dialogs") {
                waitFor(duration = Duration.ofMinutes(3), interval = Duration.ofSeconds(2)) {
                    dismissBlockingDialogs()

                    // Check if the project frame is open
                    val ideFrame = findAll<ComponentFixture>(
                        byXpath("//div[@class='IdeFrameImpl']")
                    )
                    if (ideFrame.isNotEmpty()) {
                        println("Found IdeFrameImpl")
                        true
                    } else {
                        // Dump what top-level components exist so we can diagnose CI failures
                        val allComponents = findAll<ComponentFixture>(byXpath("//div"))
                        val classNames = allComponents.mapNotNull { fixture ->
                            try {
                                fixture.callJs<String>("component.getClass().getName()")
                            } catch (_: Exception) {
                                null
                            }
                        }.distinct()
                        println("Waiting for IdeFrameImpl... Found components: ${classNames.take(30)}")

                        // If stuck on Welcome screen, the project didn't auto-open
                        val welcomeFrame = findAll<ComponentFixture>(
                            byXpath("//div[@class='FlatWelcomeFrame']")
                        )
                        if (welcomeFrame.isNotEmpty()) {
                            println("Detected Welcome screen - project did not auto-open")
                        }

                        false
                    }
                }
            }

            step("Open Module Maker via Find Action") {
                find<ComponentFixture>(
                    byXpath("//div[@class='IdeFrameImpl']"),
                    Duration.ofSeconds(10)
                ).click()

                val isMac = System.getProperty("os.name").contains("Mac", ignoreCase = true)
                keyboard {
                    if (isMac) {
                        hotKey(KeyEvent.VK_META, KeyEvent.VK_SHIFT, KeyEvent.VK_A)
                    } else {
                        hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_SHIFT, KeyEvent.VK_A)
                    }
                }
                Thread.sleep(1_000)
                keyboard { enterText("Module Maker") }
                Thread.sleep(500)
                keyboard { hotKey(KeyEvent.VK_ENTER) }
            }

            step("Verify Module Maker dialog opened") {
                waitFor(duration = Duration.ofSeconds(15)) {
                    findAll<ComponentFixture>(
                        byXpath("//div[@title='Module Maker']")
                    ).isNotEmpty()
                }
            }

            val dialog = find<CommonContainerFixture>(
                byXpath("//div[@title='Module Maker']"),
                Duration.ofSeconds(10)
            )

            val isMac = System.getProperty("os.name").contains("Mac", ignoreCase = true)

            // Strategy: Use the Create button as a stable anchor point, then
            // Shift+Tab backwards into the compose fields. The layout order
            // (bottom-up) is: Create button → Module Name → Package Name.
            // This avoids fragile forward-tabbing from the top of the dialog.

            step("Focus Module Name via Shift+Tab from Create button") {
                // Focus (don't click!) the Create button as a stable tab-order anchor
                dialog.find<ComponentFixture>(
                    byXpath("//div[@text='Create']"),
                    Duration.ofSeconds(10)
                ).callJs<Boolean>("component.requestFocusInWindow(); true")
                Thread.sleep(500)

                // Shift+Tab from Create → lands on Module Name field
                for (i in 1..SHIFT_TABS_CREATE_TO_MODULE_NAME) {
                    dispatchKey(dialog, KeyEvent.VK_TAB, shift = true)
                    Thread.sleep(150)
                }
                Thread.sleep(300)
            }

            step("Type module name") {
                dispatchText(dialog, ":repository")
                Thread.sleep(300)
            }

            step("Shift+Tab to Package Name and type") {
                dispatchKey(dialog, KeyEvent.VK_TAB, shift = true)
                Thread.sleep(300)

                // Select all existing text and replace
                if (isMac) {
                    dispatchKey(dialog, KeyEvent.VK_A, meta = true)
                } else {
                    dispatchKey(dialog, KeyEvent.VK_A, ctrl = true)
                }
                Thread.sleep(200)
                dispatchText(dialog, "com.example")
                Thread.sleep(300)
            }

            step("Click Create button") {
                dialog.find<JButtonFixture>(
                    byXpath("//div[@text='Create']"),
                    Duration.ofSeconds(10)
                ).click()
            }

            step("Dismiss any post-creation dialog") {
                Thread.sleep(3_000)
                // Try to dismiss success or error dialog
                findAll<ComponentFixture>(
                    byXpath("//div[@text='Okay']")
                ).firstOrNull()?.click()
                findAll<JButtonFixture>(
                    byXpath("//div[@class='JButton' and @text='OK']")
                ).firstOrNull()?.click()
                Thread.sleep(500)
            }

            step("Verify module was created on disk") {
                val testProjectDir = File(System.getProperty("user.dir"))
                    .resolve("src/uiTest/testProject")
                val repositoryDir = testProjectDir.resolve("repository")
                assert(repositoryDir.exists() && repositoryDir.isDirectory) {
                    "Expected repository directory at ${repositoryDir.absolutePath}"
                }
                val buildFile = repositoryDir.resolve("build.gradle.kts")
                assert(buildFile.exists()) {
                    "Expected build.gradle.kts at ${buildFile.absolutePath}"
                }
                val srcDir = repositoryDir.resolve("src")
                assert(srcDir.exists() && srcDir.isDirectory) {
                    "Expected src directory at ${srcDir.absolutePath}"
                }
                println("Module creation verified: ${repositoryDir.absolutePath}")
            }
        }
    }

    // ── Dispatch helpers ───────────────────────────────────────────────────────
    // Events must be dispatched to the Skia layer component (child of ComposePanel),
    // not the ComposePanel itself. The hierarchy is:
    //   ComposePanel → [InvisibleComponent, SwingSkiaLayerComponent$contentComponent$1]
    // The Skia layer (index 1) is the actual input-handling surface.

    private fun findComposePanel(dialog: CommonContainerFixture): ComponentFixture =
        dialog.find(byXpath("//div[@class='ComposePanel']"), Duration.ofSeconds(10))

    /** Dispatch a mouse click at (x, y) relative to the compose panel. */
    private fun dispatchClick(dialog: CommonContainerFixture, x: Int, y: Int) {
        findComposePanel(dialog).callJs<Boolean>(
            """
            var target = component.getComponent(1);
            target.requestFocusInWindow();
            var now = java.lang.System.currentTimeMillis();
            target.dispatchEvent(new java.awt.event.MouseEvent(
                target, java.awt.event.MouseEvent.MOUSE_PRESSED, now, 0,
                $x, $y, 1, false, java.awt.event.MouseEvent.BUTTON1));
            target.dispatchEvent(new java.awt.event.MouseEvent(
                target, java.awt.event.MouseEvent.MOUSE_RELEASED, now + 50, 0,
                $x, $y, 1, false, java.awt.event.MouseEvent.BUTTON1));
            target.dispatchEvent(new java.awt.event.MouseEvent(
                target, java.awt.event.MouseEvent.MOUSE_CLICKED, now + 50, 0,
                $x, $y, 1, false, java.awt.event.MouseEvent.BUTTON1));
            true
        """
        )
    }

    /** Dispatch a key press+release to the Skia layer. */
    private fun dispatchKey(
        dialog: CommonContainerFixture,
        keyCode: Int,
        ctrl: Boolean = false,
        meta: Boolean = false,
        shift: Boolean = false
    ) {
        val modifiers = mutableListOf<String>()
        if (ctrl) modifiers.add("java.awt.event.InputEvent.CTRL_DOWN_MASK")
        if (meta) modifiers.add("java.awt.event.InputEvent.META_DOWN_MASK")
        if (shift) modifiers.add("java.awt.event.InputEvent.SHIFT_DOWN_MASK")
        val modExpr = if (modifiers.isEmpty()) "0" else modifiers.joinToString(" | ")

        findComposePanel(dialog).callJs<Boolean>(
            """
            var target = component.getComponent(1);
            var now = java.lang.System.currentTimeMillis();
            target.dispatchEvent(new java.awt.event.KeyEvent(
                target, java.awt.event.KeyEvent.KEY_PRESSED, now,
                $modExpr, $keyCode, java.awt.event.KeyEvent.CHAR_UNDEFINED));
            target.dispatchEvent(new java.awt.event.KeyEvent(
                target, java.awt.event.KeyEvent.KEY_RELEASED, now + 30,
                $modExpr, $keyCode, java.awt.event.KeyEvent.CHAR_UNDEFINED));
            true
        """
        )
    }

    /** Dispatch KEY_TYPED events for each character in [text]. */
    private fun dispatchText(dialog: CommonContainerFixture, text: String) {
        for (ch in text) {
            dispatchChar(dialog, ch)
            Thread.sleep(50)
        }
    }

    private fun dispatchChar(dialog: CommonContainerFixture, ch: Char) {
        // For typed characters, we need KEY_TYPED with the char value.
        // Some characters also need KEY_PRESSED/KEY_RELEASED for the compose layer.
        val (keyCode, shift) = when {
            ch in 'a'..'z' -> (KeyEvent.VK_A + (ch - 'a')) to false
            ch in 'A'..'Z' -> (KeyEvent.VK_A + (ch - 'A')) to true
            ch in '0'..'9' -> (KeyEvent.VK_0 + (ch - '0')) to false
            ch == '.' -> KeyEvent.VK_PERIOD to false
            ch == ':' -> KeyEvent.VK_SEMICOLON to true
            ch == '-' -> KeyEvent.VK_MINUS to false
            ch == '_' -> KeyEvent.VK_MINUS to true
            else -> throw IllegalArgumentException("Unsupported character: '$ch'")
        }

        val modExpr = if (shift) "java.awt.event.InputEvent.SHIFT_DOWN_MASK" else "0"

        findComposePanel(dialog).callJs<Boolean>(
            """
            var target = component.getComponent(1);
            var now = java.lang.System.currentTimeMillis();
            target.dispatchEvent(new java.awt.event.KeyEvent(
                target, java.awt.event.KeyEvent.KEY_PRESSED, now,
                $modExpr, $keyCode, '$ch'));
            target.dispatchEvent(new java.awt.event.KeyEvent(
                target, java.awt.event.KeyEvent.KEY_TYPED, now + 10,
                0, java.awt.event.KeyEvent.VK_UNDEFINED, '$ch'));
            target.dispatchEvent(new java.awt.event.KeyEvent(
                target, java.awt.event.KeyEvent.KEY_RELEASED, now + 30,
                $modExpr, $keyCode, '$ch'));
            true
        """
        )
    }

    // ── Utilities ──────────────────────────────────────────────────────────────

    private fun RemoteRobot.dismissBlockingDialogs() {
        // Trust project dialogs
        listOf("Trust Project", "Trust and Open Project", "Trust Projects").forEach { label ->
            findAll<ComponentFixture>(byXpath("//div[@text='$label']")).firstOrNull()?.click()
        }
        // Data sharing / telemetry
        findAll<ComponentFixture>(byXpath("//div[@text=\"Don't Send\"]")).firstOrNull()?.click()
        // Generic OK/Close buttons on any dialog
        findAll<JButtonFixture>(byXpath("//div[@class='JButton' and @text='OK']")).firstOrNull()?.click()
        findAll<JButtonFixture>(byXpath("//div[@class='JButton' and @text='Close']")).firstOrNull()?.click()
        // Tip of the Day
        findAll<JButtonFixture>(byXpath("//div[@class='JButton' and @text='Got It']")).firstOrNull()?.click()
        // What's New / changelog
        findAll<ComponentFixture>(byXpath("//div[@text='Got It']")).firstOrNull()?.click()
        // Import Settings dialog
        findAll<ComponentFixture>(byXpath("//div[@text='Do not import settings']")).firstOrNull()?.click()
        findAll<ComponentFixture>(byXpath("//div[@text='Skip Import']")).firstOrNull()?.click()
        // License agreement
        findAll<ComponentFixture>(byXpath("//div[@text='Accept']")).firstOrNull()?.click()
        // Any "Continue" or "Skip" buttons
        findAll<ComponentFixture>(byXpath("//div[@text='Continue']")).firstOrNull()?.click()
        findAll<ComponentFixture>(byXpath("//div[@text='Skip Remaining and Set Defaults']")).firstOrNull()?.click()
    }

    private companion object {
        // Number of Shift+Tab presses from the Create button to the Module Name field.
        // Layout bottom-up: Create → Module Name (1) → Package Name (2).
        // Adjust if buttons or fields are added between Create and Module Name.
        const val SHIFT_TABS_CREATE_TO_MODULE_NAME = 1
    }
}
