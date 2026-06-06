package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.AnalyzerViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    assertEquals(context.getString(R.string.app_name), appName)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `test export tracking state flow`() = runTest {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = AnalyzerViewModel(application)

    // Verify initial values
    assertFalse(viewModel.uiState.value.showExportIndicator)
    assertEquals(null, viewModel.uiState.value.exportType)

    // Trigger export tracking
    viewModel.startExportTracking("PDF", "Compiling PDF", durationMs = 50L)
    assertTrue(viewModel.uiState.value.showExportIndicator)
    assertEquals("PDF", viewModel.uiState.value.exportType)
    assertEquals("Compiling PDF", viewModel.uiState.value.exportStatusMessage)

    // Complete export tracking
    viewModel.completeExportTracking("PDF", "Completed PDF successfully", success = true, autoDismissDelayMs = 0L)
    assertEquals("Completed PDF successfully", viewModel.uiState.value.exportStatusMessage)
    assertEquals(1.0f, viewModel.uiState.value.exportProgress)

    // Dismiss indicator manually
    viewModel.dismissExportIndicator()
    assertFalse(viewModel.uiState.value.showExportIndicator)
  }
}
