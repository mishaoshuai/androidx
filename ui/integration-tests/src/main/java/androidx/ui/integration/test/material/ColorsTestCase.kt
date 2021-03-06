/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.integration.test.material

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.state
import androidx.compose.ui.graphics.Color
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.lightColors
import androidx.ui.test.ComposeTestCase
import androidx.ui.integration.test.ToggleableTestCase

/**
 * Generic test case for asserting / benchmarking recomposition performance when reading values
 * from a [Colors] provided through [MaterialTheme].
 */
sealed class ColorsTestCase : ComposeTestCase, ToggleableTestCase {
    private var primaryState: MutableState<Color>? = null

    private val primaryTracker = CompositionTracker()
    private val secondaryTracker = CompositionTracker()

    @Composable
    override fun emitContent() {
        val primary = state { Color.Red }
        primaryState = primary

        val palette = createColors(primary.value)

        App(palette, primaryTracker = primaryTracker, secondaryTracker = secondaryTracker)
    }

    override fun toggleState() {
        with(primaryState!!) {
            value = if (value == Color.Blue) Color.Red else Color.Blue
        }
    }

    abstract fun createColors(primary: Color): Colors

    val primaryCompositions get() = primaryTracker.compositions
    val secondaryCompositions get() = secondaryTracker.compositions
}

/**
 * Test case using the default observable [Colors] that will be memoized and mutated when
 * incoming values change, causing only functions consuming the specific changed color to recompose.
 */
class ObservableColorsTestCase : ColorsTestCase() {
    override fun createColors(primary: Color): Colors {
        return lightColors(primary = primary)
    }
}

/**
 * Test case using an immutable [Colors], that will cause a new value to be assigned to the
 * ambient every time we change this object, causing everything consuming this ambient to recompose.
 */
class ImmutableColorsTestCase : ColorsTestCase() {
    override fun createColors(primary: Color): Colors =
        ImmutableColors(primary = primary)

    private class ImmutableColors(override val primary: Color) : Colors {
        override val secondary = Color.Black
        override val primaryVariant = Color.Black
        override val secondaryVariant = Color.Black
        override val background = Color.Black
        override val surface = Color.Black
        override val error = Color.Black
        override val onPrimary = Color.Black
        override val onSecondary = Color.Black
        override val onBackground = Color.Black
        override val onSurface = Color.Black
        override val onError = Color.Black
        override val isLight = true
    }
}

@Composable
private fun App(
    colors: Colors,
    primaryTracker: CompositionTracker,
    secondaryTracker: CompositionTracker
) {
    MaterialTheme(colors) {
        CheapPrimaryColorConsumer(primaryTracker)
        ExpensiveSecondaryColorConsumer(secondaryTracker)
        CheapPrimaryColorConsumer(primaryTracker)
    }
}

@Composable
private fun CheapPrimaryColorConsumer(compositionTracker: CompositionTracker) {
    val primary = MaterialTheme.colors.primary
    // Consume color variable to avoid any optimizations
    println("Color $primary")
    compositionTracker.compositions++
}

@Composable
private fun ExpensiveSecondaryColorConsumer(compositionTracker: CompositionTracker) {
    val secondary = MaterialTheme.colors.secondary
    // simulate some (relatively) expensive work
    Thread.sleep(1)
    // Consume color variable to avoid any optimizations
    println("Color $secondary")
    compositionTracker.compositions++
}

/**
 * Immutable as we want to ensure that we always skip recomposition unless the ambient value
 * inside the function changes.
 */
@Immutable
private class CompositionTracker(var compositions: Int = 0)
