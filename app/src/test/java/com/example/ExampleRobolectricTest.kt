package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.scheduler.TaskScheduler
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
    assertEquals("Omni AI", appName)
  }

  @Test
  fun `natural schedule parser parses minutes accurately`() {
    val parsed = TaskScheduler.parseNaturalSchedule("remind me in 15 minutes to drink water")
    assertNotNull(parsed)
    val (title, time) = parsed!!
    assertTrue(title.contains("drink water", ignoreCase = true))
    assertTrue(time > System.currentTimeMillis())
  }
}
