package com.tubers.blacklight.ui.screens

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.tubers.blacklight.AppLinks
import com.tubers.blacklight.R
import com.tubers.blacklight.ui.components.UvPanel
import com.tubers.blacklight.ui.components.openExternal
import com.tubers.blacklight.ui.components.readableWidth
import com.tubers.blacklight.ui.components.verticalFadeEdges
import com.tubers.blacklight.ui.theme.LocalAccent
import com.tubers.blacklight.ui.theme.UvColors
import com.tubers.blacklight.ui.theme.UvType

/** Policy sections in reading order: heading and body string resources. */
private val Sections = listOf(
    R.string.privacy_what_heading to R.string.privacy_what_body,
    R.string.privacy_collect_heading to R.string.privacy_collect_body,
    R.string.privacy_device_heading to R.string.privacy_device_body,
    R.string.privacy_camera_heading to R.string.privacy_camera_body,
    R.string.privacy_purchases_heading to R.string.privacy_purchases_body,
    R.string.privacy_children_heading to R.string.privacy_children_body,
    R.string.privacy_changes_heading to R.string.privacy_changes_body,
)

/**
 * In-app privacy policy (Settings › About & safety › Privacy policy). The header with the back
 * button is drawn by the root like other sub-screens; text lives in strings.xml for translation.
 */
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit, onEmailFailed: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxSize()
            .verticalFadeEdges()
            .verticalScroll(rememberScrollState())
            .readableWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 4.dp, bottom = 24.dp),
    ) {
        Text(stringResource(R.string.privacy_last_updated), style = UvType.lede, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))

        UvPanel {
            Spacer(Modifier.height(6.dp))
            Sections.forEach { (heading, body) -> PolicySection(heading) { Body(stringResource(body)) } }
            PolicySection(R.string.privacy_contact_heading, last = true) {
                val email = AppLinks.CONTACT_EMAIL
                Text(
                    stringResource(R.string.privacy_contact_body, email),
                    style = UvType.body.copy(color = LocalAccent.current.text, textDecoration = TextDecoration.Underline),
                    modifier = Modifier.clickable(role = Role.Button, onClickLabel = "Email $email") {
                        if (!context.openExternal("mailto:$email")) onEmailFailed()
                    },
                )
            }
        }
    }
}

@Composable
private fun PolicySection(@StringRes heading: Int, last: Boolean = false, content: @Composable () -> Unit) {
    Column(Modifier.padding(bottom = if (last) 4.dp else 18.dp)) {
        Text(
            stringResource(heading), style = UvType.bodyStrong, color = UvColors.Text,
            modifier = Modifier.padding(bottom = 6.dp).semantics { heading() },
        )
        content()
    }
}

@Composable
private fun Body(text: String) {
    Text(text, style = UvType.body, color = UvColors.TextBody)
}
