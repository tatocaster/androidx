/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.ui.res

import androidx.compose.runtime.Providers
import androidx.compose.ui.platform.AmbientContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontListFontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Typeface
import androidx.compose.ui.text.font.test.R
import androidx.compose.ui.text.font.toFontFamily
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class FontResourcesTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun loadFontResource_systemFontFamily() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        var result: DeferredResource<Typeface>? = null
        var syncLoadedTypeface: Typeface? = null

        rule.setContent {
            Providers(AmbientContext provides context) {

                // async API
                result = loadFontResource(
                    fontFamily = FontFamily.Monospace,
                    pendingFontFamily = FontFamily.Serif,
                    failedFontFamily = FontFamily.SansSerif
                )

                // sync API
                syncLoadedTypeface = fontResource(FontFamily.Monospace)
            }
        }

        rule.runOnIdle {
            assertThat(result).isNotNull()
            assertThat(result!!.state).isEqualTo(LoadingState.LOADED)
            assertThat(result!!.resource.resource).isEqualTo(
                syncLoadedTypeface
            )
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun loadFontResource_systemFontFamily_FileListFamily_as_pendingFontFamily() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        rule.setContent {
            Providers(AmbientContext provides context) {
                loadFontResource(
                    fontFamily = Font(R.font.sample_font).toFontFamily(),
                    pendingFontFamily = Font(R.font.sample_font).toFontFamily(),
                    failedFontFamily = FontFamily.SansSerif
                )
            }
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun loadFontResource_systemFontFamily_FileListFamily_as_failedFontFamily() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        rule.setContent {
            Providers(AmbientContext provides context) {
                loadFontResource(
                    fontFamily = Font(R.font.sample_font).toFontFamily(),
                    pendingFontFamily = FontFamily.Serif,
                    failedFontFamily = Font(R.font.sample_font).toFontFamily()
                )
            }
        }
    }

    @Test
    fun FontListFontFamily_cacheKey() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        assertThat(
            (Font(R.font.sample_font).toFontFamily() as FontListFontFamily).cacheKey(context)
        ).isEqualTo(
            (Font(R.font.sample_font).toFontFamily() as FontListFontFamily).cacheKey(context)
        )

        assertThat(
            (Font(R.font.sample_font).toFontFamily() as FontListFontFamily).cacheKey(context)
        ).isNotEqualTo(
            (Font(R.font.sample_font2).toFontFamily() as FontListFontFamily).cacheKey(context)
        )

        val fontFamily = FontFamily(
            Font(R.font.sample_font, FontWeight.Normal),
            Font(R.font.sample_font2, FontWeight.Bold)
        ) as FontListFontFamily
        assertThat(fontFamily.cacheKey(context)).isNotEqualTo(
            (Font(R.font.sample_font).toFontFamily() as FontListFontFamily).cacheKey(context)
        )
    }
}