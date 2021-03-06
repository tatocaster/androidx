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
@file:Suppress("DEPRECATION_ERROR")

package androidx.compose.foundation.text

import androidx.compose.foundation.Interaction
import androidx.compose.foundation.InteractionState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.selection.TextFieldSelectionHandle
import androidx.compose.foundation.text.selection.TextFieldSelectionManager
import androidx.compose.foundation.text.selection.isSelectionHandleInVisibleBound
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.emptyContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.savedinstancestate.rememberSavedInstanceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.isFocused
import androidx.compose.ui.gesture.scrollorientationlocking.Orientation
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.MouseTemporaryApi
import androidx.compose.ui.input.pointer.isMouseInput
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.AmbientClipboardManager
import androidx.compose.ui.platform.AmbientDensity
import androidx.compose.ui.platform.AmbientFontLoader
import androidx.compose.ui.platform.AmbientHapticFeedback
import androidx.compose.ui.platform.AmbientTextInputService
import androidx.compose.ui.platform.AmbientTextToolbar
import androidx.compose.ui.selection.AmbientTextSelectionColors
import androidx.compose.ui.selection.SimpleLayout
import androidx.compose.ui.semantics.copyText
import androidx.compose.ui.semantics.cutText
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.getTextLayoutResult
import androidx.compose.ui.semantics.imeAction
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.pasteText
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setSelection
import androidx.compose.ui.semantics.setText
import androidx.compose.ui.semantics.text
import androidx.compose.ui.semantics.textSelectionRange
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.SoftwareKeyboardController
import androidx.compose.ui.text.TextDelegate
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.input.EditProcessor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.NO_SESSION
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TextInputService
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Density
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Base composable that enables users to edit text via hardware or software keyboard.
 *
 * This composable provides basic text editing functionality, however does not include any
 * decorations such as borders, hints/placeholder.
 *
 * If the editable text is larger than the size of the container, the vertical scrolling
 * behaviour will be automatically applied. To enable a single line behaviour with horizontal
 * scrolling instead, set the [maxLines] parameter to 1, [softWrap] to false, and
 * [ImeOptions.singleLine] to true.
 *
 * Whenever the user edits the text, [onValueChange] is called with the most up to date state
 * represented by [TextFieldValue]. [TextFieldValue] contains the text entered by user, as well
 * as selection, cursor and text composition information. Please check [TextFieldValue] for the
 * description of its contents.
 *
 * It is crucial that the value provided in the [onValueChange] is fed back into [CoreTextField] in
 * order to have the final state of the text being displayed. Example usage:
 * @sample androidx.compose.foundation.samples.CoreTextFieldSample
 *
 * Please keep in mind that [onValueChange] is useful to be informed about the latest state of the
 * text input by users, however it is generally not recommended to modify the values in the
 * [TextFieldValue] that you get via [onValueChange] callback. Any change to the values in
 * [TextFieldValue] may result in a context reset and end up with input session restart. Such
 * a scenario would cause glitches in the UI or text input experience for users.
 *
 * @param value The [androidx.compose.ui.text.input.TextFieldValue] to be shown in the [CoreTextField].
 * @param onValueChange Called when the input service updates the values in [TextFieldValue].
 * @param modifier optional [Modifier] for this text field.
 * @param textStyle Style configuration that applies at character level such as color, font etc.
 * @param onImeActionPerformed Called when the input service requested an IME action. When the
 * input service emitted an IME action, this callback is called with the emitted IME action. Note
 * that this IME action may be different from what you specified in [imeAction].
 * @param visualTransformation The visual transformation filter for changing the visual
 * representation of the input. By default no visual transformation is applied.
 * @param onTextLayout Callback that is executed when a new text layout is calculated.
 * @param onTextInputStarted Callback that is executed when the initialization has done for
 * communicating with platform text input service, e.g. software keyboard on Android. Called with
 * [SoftwareKeyboardController] instance which can be used for requesting input show/hide software
 * keyboard.
 * @param interactionState The [InteractionState] representing the different [Interaction]s
 * present on this TextField. You can create and pass in your own remembered [InteractionState]
 * if you want to read the [InteractionState] and customize the appearance / behavior of this
 * TextField in different [Interaction]s.
 * @param cursorColor Color of the cursor. If [Color.Unspecified], there will be no cursor drawn
 * @param softWrap Whether the text should break at soft line breaks. If false, the glyphs in the
 * text will be positioned as if there was unlimited horizontal space.
 * @param maxLines The maximum height in terms of maximum number of visible lines. Should be
 * equal or greater than 1.
 * @param imeOptions Contains different IME configuration options.
 * @param enabled controls the enabled state of the text field. When `false`, the text
 * field will be neither editable nor focusable, the input of the text field will not be selectable
 * @param readOnly controls the editable state of the [CoreTextField]. When `true`, the text
 * field can not be modified, however, a user can focus it and copy text from it. Read-only text
 * fields are usually used to display pre-filled forms that user can not edit
 * @param decorationBox Composable lambda that allows to add decorations around text field, such
 * as icon, placeholder, helper messages or similar, and automatically increase the hit target area
 * of the text field. To allow you to control the placement of the inner text field relative to your
 * decorations, the text field implementation will pass in a framework-controlled composable
 * parameter "innerTextField" to the decorationBox lambda you provide. You must call
 * innerTextField exactly once.
 */
@Composable
@OptIn(
    ExperimentalTextApi::class,
    MouseTemporaryApi::class
)
@InternalTextApi
fun CoreTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle.Default,
    onImeActionPerformed: (ImeAction) -> Unit = {},
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    onTextInputStarted: (SoftwareKeyboardController) -> Unit = {},
    interactionState: InteractionState? = null,
    cursorColor: Color = Color.Unspecified,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    imeOptions: ImeOptions = ImeOptions.Default,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit =
        @Composable { innerTextField -> innerTextField() }
) {
    // If developer doesn't pass new value to TextField, recompose won't happen but internal state
    // and IME may think it is updated. To fix this inconsistent state, enforce recompose.
    val scope = currentRecomposeScope
    val focusRequester = FocusRequester()

    // Ambients
    // If the text field is disabled or read-only, we should not deal with the input service
    val textInputService = if (!enabled || readOnly) null else AmbientTextInputService.current
    val density = AmbientDensity.current
    val resourceLoader = AmbientFontLoader.current
    val selectionBackgroundColor = AmbientTextSelectionColors.current.backgroundColor

    // Scroll state
    val singleLine = maxLines == 1 && !softWrap && imeOptions.singleLine
    val orientation = if (singleLine) Orientation.Horizontal else Orientation.Vertical
    val scrollerPosition = rememberSavedInstanceState(
        orientation,
        saver = TextFieldScrollerPosition.Saver
    ) { TextFieldScrollerPosition(orientation) }

    // State
    val transformedText = remember(value, visualTransformation) {
        val transformed = visualTransformation.filter(value.annotatedString)
        value.composition?.let {
            TextFieldDelegate.applyCompositionDecoration(it, transformed)
        } ?: transformed
    }

    val visualText = transformedText.text
    val offsetMapping = transformedText.offsetMapping

    val state = remember {
        TextFieldState(
            TextDelegate(
                text = visualText,
                style = textStyle,
                softWrap = softWrap,
                density = density,
                resourceLoader = resourceLoader
            )
        )
    }
    state.update(
        visualText,
        textStyle,
        softWrap,
        density,
        resourceLoader,
        onValueChange,
        onImeActionPerformed,
        selectionBackgroundColor
    )

    val onValueChangeWrapper: (TextFieldValue) -> Unit = {
        state.onValueChange(it)
        scope.invalidate()
    }
    val onImeActionPerformedWrapper: (ImeAction) -> Unit = {
        state.onImeActionPerformed(it)
    }

    state.processor.onNewState(value, textInputService, state.inputSession)

    val manager = remember { TextFieldSelectionManager() }
    manager.offsetMapping = offsetMapping
    manager.onValueChange = onValueChangeWrapper
    manager.state = state
    manager.value = value
    manager.clipboardManager = AmbientClipboardManager.current
    manager.textToolbar = AmbientTextToolbar.current
    manager.hapticFeedBack = AmbientHapticFeedback.current
    manager.focusRequester = focusRequester
    manager.editable = !readOnly

    // Focus
    val focusModifier = Modifier.textFieldFocusModifier(
        enabled = enabled,
        focusRequester = focusRequester,
        interactionState = interactionState
    ) {
        if (state.hasFocus == it.isFocused) {
            return@textFieldFocusModifier
        }
        state.hasFocus = it.isFocused

        if (textInputService != null) {
            notifyTextInputServiceOnFocusChange(
                textInputService,
                state,
                value,
                imeOptions,
                onValueChangeWrapper,
                onImeActionPerformedWrapper,
                onTextInputStarted,
                offsetMapping
            )
        }
        if (!it.isFocused) manager.deselect()
    }

    val dragPositionGestureModifier = Modifier.dragPositionGestureFilter(
        interactionState = interactionState,
        enabled = enabled,
        onTap = {
            tapToFocus(state, focusRequester, textInputService, !readOnly)
        },
        onPress = {
            if (state.hasFocus) {
                state.selectionIsOn = false
                manager.hideSelectionToolbar()
            }
        },
        onRelease = {
            if (state.hasFocus && !state.selectionIsOn) {
                state.layoutResult?.let { layoutResult ->
                    TextFieldDelegate.setCursorOffset(
                        it,
                        layoutResult,
                        state.processor,
                        offsetMapping,
                        onValueChangeWrapper
                    )
                }
            }
        }
    )

    val selectionModifier =
        Modifier.longPressDragGestureFilter(manager.touchSelectionObserver, enabled)

    val pointerModifier = if (isMouseInput) {
        Modifier.mouseDragGestureFilter(
            manager.mouseSelectionObserver(onStart = { focusRequester.requestFocus() }),
            enabled = enabled
        )
    } else {
        dragPositionGestureModifier
            .then(selectionModifier)
    }

    val drawModifier = Modifier.drawBehind {
        state.layoutResult?.let { layoutResult ->
            drawIntoCanvas { canvas ->
                TextFieldDelegate.draw(
                    canvas,
                    value,
                    offsetMapping,
                    layoutResult.value,
                    state.selectionPaint
                )
            }
        }
    }

    val onPositionedModifier = Modifier.onGloballyPositioned {
        if (textInputService != null) {
            state.layoutCoordinates = it
            if (state.selectionIsOn) {
                if (state.showFloatingToolbar) {
                    manager.showSelectionToolbar()
                } else {
                    manager.hideSelectionToolbar()
                }
                state.showSelectionHandleStart = manager.isSelectionHandleInVisibleBound(true)
                state.showSelectionHandleEnd = manager.isSelectionHandleInVisibleBound(false)
            }
            state.layoutResult?.let { layoutResult ->
                TextFieldDelegate.notifyFocusedRect(
                    value,
                    state.textDelegate,
                    layoutResult.value,
                    it,
                    textInputService,
                    state.inputSession,
                    state.hasFocus,
                    offsetMapping
                )
            }
        }
        state.layoutResult?.innerTextFieldCoordinates = it
    }

    val semanticsModifier = Modifier.semantics {
        // focused semantics are handled by Modifier.focusable()
        this.imeAction = imeOptions.imeAction
        this.text = value.annotatedString
        this.textSelectionRange = value.selection
        if (!enabled) this.disabled()
        getTextLayoutResult {
            if (state.layoutResult != null) {
                it.add(state.layoutResult!!.value)
                true
            } else {
                false
            }
        }
        setText {
            onValueChangeWrapper(TextFieldValue(it.text, TextRange(it.text.length)))
            true
        }
        setSelection { start, end, traversalMode ->
            if (!enabled) {
                false
            } else if (start == value.selection.start && end == value.selection.end) {
                false
            } else if (start.coerceAtMost(end) >= 0 &&
                start.coerceAtLeast(end) <= value.annotatedString.length
            ) {
                // Do not show toolbar if it's a traversal mode (with the volume keys), or
                // if the cursor just moved to beginning or end.
                if (traversalMode || start == end) {
                    manager.exitSelectionMode()
                } else {
                    manager.enterSelectionMode()
                }
                onValueChangeWrapper(TextFieldValue(value.annotatedString, TextRange(start, end)))
                true
            } else {
                manager.exitSelectionMode()
                false
            }
        }
        onClick {
            // according to the documentation, we still need to provide proper semantics actions
            // even if the state is 'disabled'
            tapToFocus(state, focusRequester, textInputService, !readOnly)
            true
        }
        onLongClick {
            manager.enterSelectionMode()
            true
        }
        if (!value.selection.collapsed) {
            copyText {
                manager.copy()
                true
            }
            if (enabled && !readOnly) {
                cutText {
                    manager.cut()
                    true
                }
            }
        }
        if (enabled && !readOnly) {
            pasteText {
                manager.paste()
                true
            }
        }
    }

    val cursorModifier =
        Modifier.cursor(state, value, offsetMapping, cursorColor, enabled && !readOnly)

    DisposableEffect(manager) {
        onDispose { manager.hideSelectionToolbar() }
    }

    // Modifiers that should be applied to the outer text field container. Usually those include
    // gesture and semantics modifiers.
    val decorationBoxModifier = modifier
        .then(pointerModifier)
        .textFieldScrollable(scrollerPosition, interactionState, enabled)
        .then(semanticsModifier)
        .then(focusModifier)
        .onGloballyPositioned {
            state.layoutResult?.decorationBoxCoordinates = it
        }

    Box(modifier = decorationBoxModifier, propagateMinConstraints = true) {
        decorationBox {
            // Modifiers applied directly to the internal input field implementation. In general,
            // these will most likely include draw, layout and IME related modifiers.
            val coreTextFieldModifier = Modifier
                .maxLinesHeight(maxLines, textStyle)
                .textFieldScroll(
                    scrollerPosition,
                    value,
                    visualTransformation,
                    { state.layoutResult }
                )
                .then(cursorModifier)
                .then(drawModifier)
                .then(onPositionedModifier)
                .textFieldMinSize(textStyle)
                .textFieldKeyboardModifier(manager)

            SimpleLayout(coreTextFieldModifier) {
                Layout(emptyContent()) { _, constraints ->
                    TextFieldDelegate.layout(
                        state.textDelegate,
                        constraints,
                        layoutDirection,
                        state.layoutResult?.value
                    ).let { (width, height, result) ->
                        if (state.layoutResult?.value != result) {
                            state.layoutResult = TextLayoutResultProxy(result)
                            onTextLayout(result)
                        }
                        layout(
                            width,
                            height,
                            mapOf(
                                FirstBaseline to result.firstBaseline.roundToInt(),
                                LastBaseline to result.lastBaseline.roundToInt()
                            )
                        ) {}
                    }
                }

                SelectionToolbarAndHandles(
                    manager = manager,
                    show = enabled &&
                        state.hasFocus &&
                        state.selectionIsOn &&
                        state.layoutCoordinates != null &&
                        state.layoutCoordinates!!.isAttached &&
                        !isMouseInput
                )
            }
        }
    }
}

internal expect fun Modifier.textFieldKeyboardModifier(manager: TextFieldSelectionManager): Modifier

@OptIn(InternalTextApi::class)
internal class TextFieldState(
    var textDelegate: TextDelegate
) {
    val processor = EditProcessor()
    var inputSession = NO_SESSION

    /**
     * This should be a state as every time we update the value we need to redraw it.
     * state observation during onDraw callback will make it work.
     */
    var hasFocus by mutableStateOf(false)

    /** The last layout coordinates for the Text's layout, used by selection */
    var layoutCoordinates: LayoutCoordinates? = null

    /**
     * You should be using proxy type [TextLayoutResultProxy] if you need to translate touch
     * offset into text's coordinate system. For example, if you add a gesture on top of the
     * decoration box and want to know the character in text for the given touch offset on
     * decoration box.
     * When you don't need to shift the touch offset, you should be using `layoutResult.value`
     * which omits the proxy and calls the layout result directly. This is needed when you work
     * with the text directly, and not the decoration box. For example, cursor modifier gets
     * position using the [TextFieldValue.selection] value which corresponds to the text directly,
     * and therefore does not require the translation.
     */
    var layoutResult: TextLayoutResultProxy? = null

    /**
     * The gesture detector status, to indicate whether current status is selection or editing.
     *
     * In the editing mode, there is no selection shown, only cursor is shown. To enter the editing
     * mode from selection mode, just tap on the screen.
     *
     * In the selection mode, there is no cursor shown, only selection is shown. To enter
     * the selection mode, just long press on the screen. In this mode, finger movement on the
     * screen changes selection instead of moving the cursor.
     */
    var selectionIsOn by mutableStateOf(false)

    /**
     * A flag to check if the selection start or end handle is being dragged.
     * If this value is true, then onPress will not select any text.
     * This value will be set to true when either handle is being dragged, and be reset to false
     * when the dragging is stopped.
     */
    var draggingHandle = false

    /**
     * A flag to check if the floating toolbar should show.
     */
    var showFloatingToolbar = false

    /**
     * A flag to check if the start selection handle should show.
     */
    var showSelectionHandleStart by mutableStateOf(false)

    /**
     * A flag to check if the end selection handle should show.
     */
    var showSelectionHandleEnd by mutableStateOf(false)

    var onImeActionPerformed: (ImeAction) -> Unit = {}
        private set

    var onValueChange: (TextFieldValue) -> Unit = {}
        private set

    /** The paint used to draw highlight background for selected text. */
    val selectionPaint: Paint = Paint()

    fun update(
        visualText: AnnotatedString,
        textStyle: TextStyle,
        softWrap: Boolean,
        density: Density,
        resourceLoader: Font.ResourceLoader,
        onValueChange: (TextFieldValue) -> Unit,
        onImeActionPerformed: (ImeAction) -> Unit,
        selectionBackgroundColor: Color
    ) {
        this.onValueChange = onValueChange
        this.onImeActionPerformed = onImeActionPerformed
        this.selectionPaint.color = selectionBackgroundColor

        textDelegate = updateTextDelegate(
            current = textDelegate,
            text = visualText,
            style = textStyle,
            softWrap = softWrap,
            density = density,
            resourceLoader = resourceLoader,
            placeholders = emptyList()
        )
    }
}

/**
 * Request focus on tap. If already focused, makes sure the keyboard is requested.
 */
private fun tapToFocus(
    state: TextFieldState,
    focusRequester: FocusRequester,
    textInputService: TextInputService?,
    allowKeyboard: Boolean
) {
    if (!state.hasFocus) {
        focusRequester.requestFocus()
    } else if (allowKeyboard) {
        textInputService?.showSoftwareKeyboard(state.inputSession)
    }
}

@OptIn(InternalTextApi::class)
private fun notifyTextInputServiceOnFocusChange(
    textInputService: TextInputService,
    state: TextFieldState,
    value: TextFieldValue,
    imeOptions: ImeOptions,
    onValueChange: (TextFieldValue) -> Unit,
    onImeActionPerformed: (ImeAction) -> Unit,
    onTextInputStarted: (SoftwareKeyboardController) -> Unit,
    offsetMapping: OffsetMapping
) {
    if (state.hasFocus) {
        state.inputSession = TextFieldDelegate.onFocus(
            textInputService,
            value,
            state.processor,
            imeOptions,
            onValueChange,
            onImeActionPerformed
        )
        if (state.inputSession != NO_SESSION) {
            onTextInputStarted(SoftwareKeyboardController(textInputService, state.inputSession))
        }
        state.layoutCoordinates?.let { coords ->
            state.layoutResult?.let { layoutResult ->
                TextFieldDelegate.notifyFocusedRect(
                    value,
                    state.textDelegate,
                    layoutResult.value,
                    coords,
                    textInputService,
                    state.inputSession,
                    state.hasFocus,
                    offsetMapping
                )
            }
        }
    } else {
        TextFieldDelegate.onBlur(
            textInputService,
            state.inputSession,
            state.processor,
            false,
            onValueChange
        )
    }
}

@Composable
private fun SelectionToolbarAndHandles(manager: TextFieldSelectionManager, show: Boolean) {
    if (show) {
        with(manager) {
            state?.layoutResult?.value?.let {
                if (!value.selection.collapsed) {
                    val startOffset = offsetMapping.originalToTransformed(value.selection.start)
                    val endOffset = offsetMapping.originalToTransformed(value.selection.end)
                    val startDirection = it.getBidiRunDirection(startOffset)
                    val endDirection = it.getBidiRunDirection(max(endOffset - 1, 0))
                    val directions = Pair(startDirection, endDirection)
                    if (manager.state?.showSelectionHandleStart == true) {
                        TextFieldSelectionHandle(
                            isStartHandle = true,
                            directions = directions,
                            manager = manager
                        )
                    }
                    if (manager.state?.showSelectionHandleEnd == true) {
                        TextFieldSelectionHandle(
                            isStartHandle = false,
                            directions = directions,
                            manager = manager
                        )
                    }
                }

                state?.let { textFieldState ->
                    // If in selection mode (when the floating toolbar is shown) a new symbol
                    // from the keyboard is entered, text field should enter the editing mode
                    // instead.
                    if (isTextChanged()) textFieldState.showFloatingToolbar = false
                    if (textFieldState.hasFocus) {
                        if (textFieldState.showFloatingToolbar) showSelectionToolbar()
                        else hideSelectionToolbar()
                    }
                }
            }
        }
    } else manager.hideSelectionToolbar()
}