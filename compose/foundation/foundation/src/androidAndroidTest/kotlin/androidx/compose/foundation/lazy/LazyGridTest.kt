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

package androidx.compose.foundation.lazy

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.foundation.layout.preferredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFoundationApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class LazyGridTest {
    private val LazyGridTag = "LazyGridTag"

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun lazyGridShowsOneItem() {
        val itemTestTag = "itemTestTag"

        rule.setContent {
            LazyVerticalGrid(
                cells = GridCells.Fixed(3)
            ) {
                item {
                    Spacer(
                        Modifier.preferredSize(10.dp).testTag(itemTestTag)
                    )
                }
            }
        }

        rule.onNodeWithTag(itemTestTag)
            .assertIsDisplayed()
    }

    @Test
    fun lazyGridShowsOneRow() {
        val items = (1..5).map { it.toString() }

        rule.setContent {
            LazyVerticalGrid(
                cells = GridCells.Fixed(3),
                modifier = Modifier.preferredHeight(100.dp).preferredWidth(300.dp)
            ) {
                items(items) {
                    Spacer(Modifier.preferredHeight(101.dp).testTag(it))
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertIsDisplayed()

        rule.onNodeWithTag("2")
            .assertIsDisplayed()

        rule.onNodeWithTag("3")
            .assertIsDisplayed()

        rule.onNodeWithTag("4")
            .assertDoesNotExist()

        rule.onNodeWithTag("5")
            .assertDoesNotExist()
    }

    @Test
    fun lazyGridShowsSecondRowOnScroll() {
        val items = (1..9).map { it.toString() }

        rule.setContent {
            LazyVerticalGrid(
                cells = GridCells.Fixed(3),
                modifier = Modifier.preferredHeight(100.dp).testTag(LazyGridTag)
            ) {
                items(items) {
                    Spacer(Modifier.preferredHeight(101.dp).testTag(it))
                }
            }
        }

        rule.onNodeWithTag(LazyGridTag)
            .scrollBy(y = 50.dp, density = rule.density)

        rule.onNodeWithTag("4")
            .assertIsDisplayed()

        rule.onNodeWithTag("5")
            .assertIsDisplayed()

        rule.onNodeWithTag("6")
            .assertIsDisplayed()

        rule.onNodeWithTag("7")
            .assertDoesNotExist()

        rule.onNodeWithTag("8")
            .assertDoesNotExist()

        rule.onNodeWithTag("9")
            .assertDoesNotExist()
    }

    @Test
    fun lazyGridScrollHidesFirstRow() {
        val items = (1..9).map { it.toString() }

        rule.setContent {
            LazyVerticalGrid(
                cells = GridCells.Fixed(3),
                modifier = Modifier.preferredHeight(200.dp).testTag(LazyGridTag)
            ) {
                items(items) {
                    Spacer(Modifier.preferredHeight(101.dp).testTag(it))
                }
            }
        }

        rule.onNodeWithTag(LazyGridTag)
            .scrollBy(y = 103.dp, density = rule.density)

        rule.onNodeWithTag("1")
            .assertDoesNotExist()

        rule.onNodeWithTag("2")
            .assertDoesNotExist()

        rule.onNodeWithTag("3")
            .assertDoesNotExist()

        rule.onNodeWithTag("4")
            .assertIsDisplayed()

        rule.onNodeWithTag("5")
            .assertIsDisplayed()

        rule.onNodeWithTag("6")
            .assertIsDisplayed()

        rule.onNodeWithTag("7")
            .assertIsDisplayed()

        rule.onNodeWithTag("8")
            .assertIsDisplayed()

        rule.onNodeWithTag("9")
            .assertIsDisplayed()
    }

    @Test
    fun adaptiveLazyGridFillsAllWidth() {
        val items = (1..5).map { it.toString() }

        rule.setContent {
            LazyVerticalGrid(
                cells = GridCells.Adaptive(130.dp),
                modifier = Modifier.preferredHeight(100.dp).preferredWidth(300.dp)
            ) {
                items(items) {
                    Spacer(Modifier.preferredHeight(101.dp).testTag(it))
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertLeftPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("2")
            .assertLeftPositionInRootIsEqualTo(150.dp)

        rule.onNodeWithTag("3")
            .assertDoesNotExist()

        rule.onNodeWithTag("4")
            .assertDoesNotExist()

        rule.onNodeWithTag("5")
            .assertDoesNotExist()
    }

    @Test
    fun adaptiveLazyGridAtLeastOneColumn() {
        val items = (1..3).map { it.toString() }

        rule.setContent {
            LazyVerticalGrid(
                cells = GridCells.Adaptive(301.dp),
                modifier = Modifier.preferredHeight(100.dp).preferredWidth(300.dp)
            ) {
                items(items) {
                    Spacer(Modifier.preferredHeight(101.dp).testTag(it))
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertIsDisplayed()

        rule.onNodeWithTag("2")
            .assertDoesNotExist()

        rule.onNodeWithTag("3")
            .assertDoesNotExist()
    }

    @Test
    fun usedWithArray() {
        val items = arrayOf("1", "2", "3", "4")

        val itemSize = with(rule.density) { 15.toDp() }

        rule.setContent {
            LazyVerticalGrid(
                GridCells.Fixed(2),
                Modifier.width(itemSize * 2)
            ) {
                items(items) {
                    Spacer(Modifier.height(itemSize).testTag(it))
                }
            }
        }

        rule.onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertLeftPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("2")
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertLeftPositionInRootIsEqualTo(itemSize)

        rule.onNodeWithTag("3")
            .assertTopPositionInRootIsEqualTo(itemSize)
            .assertLeftPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("4")
            .assertTopPositionInRootIsEqualTo(itemSize)
            .assertLeftPositionInRootIsEqualTo(itemSize)
    }

    @Test
    fun usedWithArrayIndexed() {
        val items = arrayOf("1", "2", "3", "4")

        val itemSize = with(rule.density) { 15.toDp() }

        rule.setContent {
            LazyVerticalGrid(
                GridCells.Fixed(2),
                Modifier.width(itemSize * 2)
            ) {
                itemsIndexed(items) { index, item ->
                    Spacer(Modifier.height(itemSize).testTag("$index*$item"))
                }
            }
        }

        rule.onNodeWithTag("0*1")
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertLeftPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("1*2")
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertLeftPositionInRootIsEqualTo(itemSize)

        rule.onNodeWithTag("2*3")
            .assertTopPositionInRootIsEqualTo(itemSize)
            .assertLeftPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithTag("3*4")
            .assertTopPositionInRootIsEqualTo(itemSize)
            .assertLeftPositionInRootIsEqualTo(itemSize)
    }
}
