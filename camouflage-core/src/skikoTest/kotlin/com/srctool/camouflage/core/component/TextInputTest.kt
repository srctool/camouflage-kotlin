package com.srctool.camouflage.core.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.button.CamoButton
import com.srctool.camouflage.core.form.CamoCheckboxFormField
import com.srctool.camouflage.core.form.CamoForm
import com.srctool.camouflage.core.form.CamoFormState
import com.srctool.camouflage.core.form.CamoRules
import com.srctool.camouflage.core.form.CamoTextFormField
import com.srctool.camouflage.core.form.ValidateMode
import com.srctool.camouflage.core.form.rememberCamoFormState
import com.srctool.camouflage.core.provider.CamouflageProvider
import com.srctool.camouflage.core.search.CamoSearchField
import com.srctool.camouflage.core.skinA
import com.srctool.camouflage.core.skinB
import com.srctool.camouflage.core.textfield.CamoDisplayFormat
import com.srctool.camouflage.core.textfield.CamoFieldAction
import com.srctool.camouflage.core.text.CamoText
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import com.srctool.camouflage.core.textfield.CamoInputFilter
import com.srctool.camouflage.core.textfield.CamoTextField
import com.srctool.camouflage.core.themeT
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// The batch-2 inputs' tests from their Kotlin notes: the text field, the search field and the form.
@OptIn(ExperimentalTestApi::class)
class TextInputTest {
    private fun ComposeUiTest.camo(content: @Composable () -> Unit) =
        setContent { CamouflageProvider(skinA, themeT, content) }

    private val input = hasSetTextAction()

    // The field labelled [field] is marked invalid with [message] (the visible error text hides its semantics).
    private fun ComposeUiTest.shows(field: String, message: String) =
        onNodeWithText(field).assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, message))

    private fun ComposeUiTest.showsNo(field: String, message: String) =
        onNodeWithText(field)
            .assert(SemanticsMatcher("no error $message") { it.config.getOrNull(SemanticsProperties.Error) != message })

    // CamoTextField

    @Test
    fun typingCallsOnValueChangeAndAnIgnoredChangeKeepsTheValue() = runComposeUiTest {
        var value by mutableStateOf("")
        var calls = 0
        camo {
            Column {
                CamoTextField(value, { value = it }, label = "Name")
                CamoTextField("fixed", { calls++ }, label = "Fixed")
            }
        }
        onNodeWithText("Name").performTextInput("a")
        waitForIdle()
        assertEquals("a", value)
        onNodeWithText("Fixed").performTextInput("x")
        waitForIdle()
        assertEquals(1, calls)
        onNodeWithText("Fixed").assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText,
            androidx.compose.ui.text.AnnotatedString("fixed")))
    }

    @Test
    fun aFilterRejectsInputBeforeTheApp() = runComposeUiTest {
        var value by mutableStateOf("")
        var calls = 0
        camo {
            CamoTextField(value, { calls++; value = it }, label = "Phone", inputFilter = CamoInputFilter.digitsOnly())
        }
        onNodeWithText("Phone").performTextInput("a")
        waitForIdle()
        assertEquals(0, calls)
        onNodeWithText("Phone").performTextReplacement("0812-3456-7890")
        waitForIdle()
        assertEquals("081234567890", value)
    }

    @Test
    fun aDisplayFormatShowsDashesKeepsTheValueRawAndTheCursorInPlace() = runComposeUiTest {
        var value by mutableStateOf("08123")
        camo {
            CamoTextField(value, { value = it }, label = "Phone", inputFilter = CamoInputFilter.digitsOnly(),
                displayFormat = CamoDisplayFormat.pattern("####-####-#####"))
        }
        onNodeWithText("Phone").assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText,
            androidx.compose.ui.text.AnnotatedString("0812-3")))
        // Cursor after the "8" (shown and raw position 3), then type 9.
        onNodeWithText("Phone").requestFocus()
        onNodeWithText("Phone").performTextInputSelection(TextRange(3))
        onNodeWithText("Phone").performTextInput("9")
        waitForIdle()
        assertEquals("081923", value)
        val selection = onNodeWithText("Phone").fetchSemanticsNode()
            .config[SemanticsProperties.TextSelectionRange]
        assertEquals(TextRange(4), selection)
    }

    @Test
    fun anErrorMarksItInvalidAndTheLabelNamesIt() = runComposeUiTest {
        camo { CamoTextField("", {}, label = "Email", error = "Enter an email", supportingText = "Work email") }
        onNodeWithText("Email").assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, "Enter an email"))
        onNodeWithText("Work email").assertDoesNotExist()
    }

    @Test
    fun switchingTheSkinWhileTypingKeepsFocusAndText() = runComposeUiTest {
        var skin by mutableStateOf(skinA)
        var value by mutableStateOf("")
        setContent { CamouflageProvider(skin, themeT) { CamoTextField(value, { value = it }, label = "Note") } }
        onNodeWithText("Note").performTextInput("hello")
        onNodeWithText("Note").assertIsFocused()
        skin = skinB
        waitForIdle()
        onNodeWithText("Note").assertIsFocused()
        assertEquals("hello", value)
    }

    // CamoSearchField

    @Test
    fun theSearchActionAndEnterCallOnSearch() = runComposeUiTest {
        var query by mutableStateOf("")
        val searched = mutableListOf<String>()
        camo { CamoSearchField(query, { query = it }, { searched += it }) }
        val field = onNode(input)
        field.performTextInput("tea")
        field.performKeyInput { pressKey(Key.Enter) }
        waitForIdle()
        assertEquals(listOf("tea"), searched)
    }

    @Test
    fun clearAppearsWithTextClearsAndKeepsFocus() = runComposeUiTest {
        var query by mutableStateOf("")
        camo { CamoSearchField(query, { query = it }, {}) }
        onNodeWithContentDescription("Clear search").assertDoesNotExist()
        onNode(input).performTextInput("tea")
        onNodeWithContentDescription("Clear search").performClick()
        waitForIdle()
        assertEquals("", query)
        onNode(input).assertIsFocused()
    }

    @Test
    fun anExternalQueryChangeUpdatesTheField() = runComposeUiTest {
        var query by mutableStateOf("old")
        camo { CamoSearchField(query, { query = it }, {}) }
        query = "new"
        waitForIdle()
        onNode(input).assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText,
            androidx.compose.ui.text.AnnotatedString("new")))
    }

    // CamoForm

    @Composable
    private fun SignUp(form: CamoFormState, mode: ValidateMode, saved: MutableMap<String, Any?>) {
        CamoForm(form, validateMode = mode) {
            Column {
                CamoTextFormField(key = "email", label = "Email",
                    rules = listOf(CamoRules.required("Email is required"), CamoRules.email("Enter an email")),
                    onSaved = { saved["email"] = it })
                CamoTextFormField(key = "password", label = "Password", secure = true,
                    rules = listOf(CamoRules.minLength(8, "Use at least 8 characters")),
                    onSaved = { saved["password"] = it })
                CamoTextFormField(key = "confirm", label = "Confirm",
                    rules = listOf(CamoRules.matches("password", "Passwords don't match")))
                CamoCheckboxFormField(key = "consent", label = "I agree",
                    rules = listOf(CamoRules.requiredTrue("Please accept")))
            }
        }
    }

    @Test
    fun disabledModeValidatesOnlyOnSubmitAndFocusesTheFirstInvalid() = runComposeUiTest {
        lateinit var form: CamoFormState
        camo {
            form = rememberCamoFormState()
            SignUp(form, ValidateMode.Disabled, mutableMapOf())
        }
        onNodeWithText("Email").performTextInput("not")
        waitForIdle()
        showsNo("Email", "Enter an email")
        runOnIdle { assertFalse(form.validate()) }
        waitForIdle()
        shows("Email", "Enter an email")
        shows("I agree", "Please accept")
        onNodeWithText("Email").assertIsFocused()
    }

    @Test
    fun onUserInteractionValidatesEachEditedField() = runComposeUiTest {
        camo { SignUp(rememberCamoFormState(), ValidateMode.OnUserInteraction, mutableMapOf()) }
        onNodeWithText("Password").performTextInput("short")
        waitForIdle()
        shows("Password", "Use at least 8 characters")
        showsNo("Email", "Email is required")
    }

    @Test
    fun alwaysValidatesBeforeAnyEdit() = runComposeUiTest {
        camo { SignUp(rememberCamoFormState(), ValidateMode.Always, mutableMapOf()) }
        waitForIdle()
        shows("Email", "Email is required")
    }

    @Test
    fun onUnfocusValidatesWhenFocusLeaves() = runComposeUiTest {
        camo { SignUp(rememberCamoFormState(), ValidateMode.OnUnfocus, mutableMapOf()) }
        onNodeWithText("Email").performTextInput("not")
        waitForIdle()
        showsNo("Email", "Enter an email")
        onNodeWithText("Password").requestFocus()
        waitForIdle()
        shows("Email", "Enter an email")
    }

    @Test
    fun matchesSaveServerErrorsAndDirty() = runComposeUiTest {
        lateinit var form: CamoFormState
        val saved = mutableMapOf<String, Any?>()
        camo {
            form = rememberCamoFormState()
            SignUp(form, ValidateMode.Disabled, saved)
        }
        onNodeWithText("Email").performTextInput("a@b.co")
        onNodeWithText("Password").performTextInput("longenough")
        onNodeWithText("Confirm").performTextInput("different")
        onNodeWithText("I agree").performClick()
        waitForIdle()
        runOnIdle {
            assertTrue(form.isDirty)
            assertFalse(form.validate())
        }
        waitForIdle()
        shows("Confirm", "Passwords don't match")
        onNodeWithText("Confirm").performTextReplacement("longenough")
        runOnIdle {
            assertTrue(form.validate())
            form.save()
            form.setErrors(mapOf("email" to "Already taken"))
        }
        assertEquals("a@b.co", saved["email"])
        waitForIdle()
        shows("Email", "Already taken")
        onNodeWithText("Email").performTextInput("m")
        waitForIdle()
        showsNo("Email", "Already taken")
        runOnIdle {
            form.reset()
            assertFalse(form.isDirty)
        }
    }

    @Test
    fun aHiddenFieldKeepsItsValueAndIsSkipped() = runComposeUiTest {
        lateinit var form: CamoFormState
        var shown by mutableStateOf(true)
        camo {
            form = rememberCamoFormState()
            CamoForm(form) {
                CamoTextFormField(key = "company", label = "Company", visible = shown,
                    rules = listOf(CamoRules.minLength(5, "Too short")))
            }
        }
        onNodeWithText("Company").performTextInput("abc")
        shown = false
        waitForIdle()
        runOnIdle { assertTrue(form.validate()) }
        shown = true
        waitForIdle()
        assertEquals("abc", form.value<String>("company"))
    }

    @Test
    fun aLazyFormValidatesOffScreenRowsAndRevealsTheFirstInvalid() = runComposeUiTest {
        lateinit var form: CamoFormState
        val keys = (0 until 50).map { "row$it" }
        camo {
            form = rememberCamoFormState()
            val list = rememberLazyListState()
            CamoForm(form, list, indexOfKey = { keys.indexOf(it) }) {
                LazyColumn(Modifier.height(300.dp), state = list) {
                    items(keys.size, key = { keys[it] }) { i ->
                        CamoTextFormField(key = keys[i], label = "Row $i", initialValue = if (i == 40) "" else "ok",
                            rules = listOf(CamoRules.required("Row $i is required")))
                    }
                }
            }
        }
        // Rows created off screen only exist once declared; declare the far one up front.
        runOnIdle { form.declare("row40", "", listOf(CamoRules.required("Row 40 is required"))) }
        runOnIdle { assertFalse(form.validate()) }
        waitForIdle()
        shows("Row 40", "Row 40 is required")
        onNodeWithText("Row 40").assertIsFocused()
    }

    @Test
    fun aSkinSwitchKeepsTheFormsValuesAndErrors() = runComposeUiTest {
        var skin by mutableStateOf(skinA)
        lateinit var form: CamoFormState
        setContent {
            CamouflageProvider(skin, themeT) {
                form = rememberCamoFormState()
                SignUp(form, ValidateMode.Disabled, mutableMapOf())
            }
        }
        onNodeWithText("Email").performTextInput("not")
        runOnIdle { form.validate() }
        skin = skinB
        waitForIdle()
        shows("Email", "Enter an email")
        assertEquals("not", form.value<String>("email"))
    }

    @Test
    fun aFieldActionKeepsTheFieldsHeightAndIsItsOwnButton() = runComposeUiTest {
        var shown = 0
        camo {
            Column {
                CamoTextField("", {}, Modifier.testTag("plain"), label = "Plain")
                CamoTextField("", {}, Modifier.testTag("action"), label = "With action", suffixIcon = {
                    CamoFieldAction({ CamoText("o") }, "Show password", { shown++ })
                })
            }
        }
        val plain = onNodeWithTag("plain", useUnmergedTree = true).fetchSemanticsNode().size.height
        val action = onNodeWithTag("action", useUnmergedTree = true).fetchSemanticsNode().size.height
        assertEquals(plain, action)
        onNodeWithContentDescription("Show password").performClick()
        waitForIdle()
        assertEquals(1, shown)
    }
}
