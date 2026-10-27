package com.srctool.camouflage.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.srctool.camouflage.core.button.CamoButton
import com.srctool.camouflage.core.form.CamoCheckboxFormField
import com.srctool.camouflage.core.form.CamoForm
import com.srctool.camouflage.core.form.CamoRadioGroupFormField
import com.srctool.camouflage.core.form.CamoRules
import com.srctool.camouflage.core.form.CamoStepperFormField
import com.srctool.camouflage.core.form.CamoTextFormField
import com.srctool.camouflage.core.form.ValidateMode
import com.srctool.camouflage.core.form.rememberCamoFormState
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.radio.RadioOption
import com.srctool.camouflage.core.search.CamoSearchField
import com.srctool.camouflage.core.text.CamoSpan
import com.srctool.camouflage.core.text.CamoText
import com.srctool.camouflage.core.textfield.CamoDisplayFormat
import com.srctool.camouflage.core.textfield.CamoFieldAction
import com.srctool.camouflage.core.textfield.CamoInputFilter
import com.srctool.camouflage.core.theme.Appearance
import kotlinx.coroutines.delay
import com.srctool.camouflage.core.form.CamoDateFormField
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

private val PasswordRules = listOf(
    CamoRules.required("Password is required"),
    CamoRules.minLength(8, "Use at least 8 characters"),
)

/** The base note's sign-up form: rules as objects, validated on submit and then on each edit. */
@Composable
internal fun SignUpForm() {
    val form = rememberCamoFormState()
    var result by remember { mutableStateOf<String?>(null) }
    CamoForm(form, validateMode = ValidateMode.OnUserInteraction) {
        Column(verticalArrangement = Arrangement.spacedBy(Camouflage.spacing.m)) {
            AccountFields()
            DetailFields()
            CamoRadioGroupFormField(
                key = "plan",
                label = "Plan",
                options = listOf(RadioOption("free", "Free"), RadioOption("pro", "Pro", "€ 8 a month")),
                rules = listOf(CamoRules.required("Pick a plan")),
            )
            CamoStepperFormField(key = "seats", label = "Seats", initialValue = 1, range = 1..50)
            CamoCheckboxFormField(
                key = "consent",
                labelSpans = listOf(CamoSpan("I agree to the "), CamoSpan("Terms", link = {})),
                rules = listOf(CamoRules.requiredTrue("Please accept the terms")),
            )
            CamoButton("Create account", {
                result = if (form.validate()) {
                    form.save()
                    "Account created for ${form.value<String>("email")}"
                } else {
                    null
                }
            }, Modifier.fillMaxWidth())
            CamoButton("Pretend the server says the email is taken", {
                form.setErrors(mapOf("email" to "This email already has an account"))
            }, Modifier.fillMaxWidth(), appearance = Appearance.Ghost)
            result?.let { CamoText(it, color = Camouflage.colors.success) }
        }
    }
}

// Email, a password with a show toggle, and its confirmation.
@Composable
private fun AccountFields() {
    var hidden by remember { mutableStateOf(true) }
    CamoTextFormField(
        key = "email",
        modifier = Modifier.fillMaxWidth(),
        label = "Email",
        placeholder = "you@example.com",
        supportingText = "We never share it",
        prefixIcon = { AppIcon.Search() },
        rules = listOf(CamoRules.required("Email is required"), CamoRules.email("Enter a valid email")),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
    )
    CamoTextFormField(
        key = "password",
        modifier = Modifier.fillMaxWidth(),
        label = "Password",
        secure = hidden,
        rules = PasswordRules,
        suffixIcon = {
            CamoFieldAction(
                icon = { if (hidden) AppIcon.StarOutline() else AppIcon.Star() },
                contentDescription = if (hidden) "Show password" else "Hide password",
                onClick = { hidden = !hidden },
            )
        },
    )
    CamoTextFormField(
        key = "confirm",
        modifier = Modifier.fillMaxWidth(),
        label = "Confirm password",
        secure = true,
        rules = listOf(CamoRules.matches("password", "Passwords don't match")),
    )
}

// A phone shown with dashes, an amount with thousand separators, and a disabled field.
@Composable
private fun DetailFields() {
    CamoTextFormField(
        key = "phone",
        modifier = Modifier.fillMaxWidth(),
        label = "Phone",
        prefixText = "+62",
        inputFilter = CamoInputFilter.digitsOnly(maxLength = 13),
        displayFormat = CamoDisplayFormat.pattern("####-####-#####"),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
    )
    CamoDateFormField(
        key = "birthDate",
        modifier = Modifier.fillMaxWidth(),
        label = "Birth date",
        maxDate = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) },
        rules = listOf(CamoRules.required("Enter your birth date")),
    )
    CamoTextFormField(
        key = "budget",
        modifier = Modifier.fillMaxWidth(),
        label = "Monthly budget",
        prefixText = "Rp",
        inputFilter = CamoInputFilter.digitsOnly(maxLength = 12),
        displayFormat = CamoDisplayFormat.grouping(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
    CamoTextFormField(key = "company", modifier = Modifier.fillMaxWidth(), label = "Company", enabled = false,
        initialValue = "Srctool")
}

private val Countries = listOf(
    "Argentina", "Australia", "Brazil", "Canada", "Denmark", "Egypt", "Finland", "France", "Germany", "India",
    "Indonesia", "Japan", "Kenya", "Mexico", "Netherlands", "Norway", "Peru", "Portugal", "Singapore", "Spain",
)

private const val DEBOUNCE_MILLIS = 500L

/** A country search: the app debounces the query (500 ms) and lists the matches below the field. */
@Composable
internal fun CountrySearch() {
    var query by remember { mutableStateOf("") }
    var settled by remember { mutableStateOf("") }
    var searched by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(query) {
        delay(DEBOUNCE_MILLIS)
        settled = query
    }
    CamoSearchField(query, { query = it }, { searched = it }, Modifier.fillMaxWidth(), placeholder = "Search countries")
    val matches = Countries.filter { it.contains(settled, ignoreCase = true) }
    CamoText("${matches.size} results", color = Camouflage.colors.onSurfaceVariant)
    matches.take(MAX_SHOWN).forEach { CamoText(it) }
    searched?.let { CamoText("Searched for \"$it\"", color = Camouflage.colors.onSurfaceVariant) }
}

private const val MAX_SHOWN = 6
