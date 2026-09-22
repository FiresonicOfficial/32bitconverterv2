package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.compat.SystemAbiInfo
import com.example.ui.components.SystemAbiHeader
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val sampleAbiInfo = SystemAbiInfo(
      is64BitOnly = true,
      primaryAbi = "arm64-v8a",
      supportedAbis = listOf("arm64-v8a"),
      supported64BitAbis = listOf("arm64-v8a"),
      supported32BitAbis = emptyList(),
      osArch = "aarch64",
      androidVersion = "Android 15",
      apiLevel = 35,
      compatibilityBridgeStatus = "Active (ARM32 Translation Engaged)"
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        SystemAbiHeader(
          abiInfo = sampleAbiInfo,
          gamesCount = 4
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}

