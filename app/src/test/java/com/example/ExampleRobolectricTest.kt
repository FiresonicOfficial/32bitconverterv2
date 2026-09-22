package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.compat.AbiDetector
import com.example.data.db.GameEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ARM32 Runner", appName)
  }

  @Test
  fun `verify system abi detector runs cleanly`() {
    val abiInfo = AbiDetector.getSystemAbiInfo()
    assertNotNull(abiInfo)
    assertNotNull(abiInfo.primaryAbi)
    assertTrue(abiInfo.compatibilityBridgeStatus.isNotEmpty())
  }

  @Test
  fun `verify game entity properties`() {
    val game = GameEntity(
      packageName = "com.test.arm32",
      title = "Retro Classic",
      versionName = "1.0",
      targetSdk = 19,
      abiType = "armeabi-v7a",
      isPure32Bit = true,
      apkPath = "builtin://test"
    )
    assertTrue(game.isPure32Bit)
    assertEquals("armeabi-v7a", game.abiType)
  }
}

