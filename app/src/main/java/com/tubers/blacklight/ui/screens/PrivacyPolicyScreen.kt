package com.tubers.blacklight.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    R.string.privacy_device_heading to R.string.privacy_device_body,
    R.string.privacy_ads_heading to R.string.privacy_ads_body,
    R.string.privacy_camera_heading to R.string.privacy_camera_body,
    R.string.privacy_purchases_heading to R.string.privacy_purchases_body,
    R.string.privacy_children_heading to R.string.privacy_children_body,
    R.string.privacy_sharing_heading to R.string.privacy_sharing_body,
    R.string.privacy_changes_heading to R.string.privacy_changes_body,
)

private val UrlPattern = Regex("""(https?://\S+|mailto:\S+)""")

/**
 * In-app privacy policy (Settings › About & safety › Privacy policy). The header with the back
 * button is drawn by the root like other sub-screens; text lives in strings.xml for translation.
 * Each section reads "Heading: text", exactly like the public copy in docs/privacy-policy.md.
 */
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit, onLinkFailed: () -> Unit) {
    BackHandler(onBack = onBack)
    val email = AppLinks.CONTACT_EMAIL
    val context = LocalContext.current
    val open: (String) -> Unit = { url -> if (!context.openExternal(url)) onLinkFailed() }
    Column(
        Modifier
            .fillMaxSize()
            .verticalFadeEdges()
            .verticalScroll(rememberScrollState())
            .readableWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 4.dp, bottom = 24.dp),
    ) {
        Text(
            stringResource(R.string.privacy_title), fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = UvColors.Text,
            modifier = Modifier.padding(top = 4.dp).semantics { heading() },
        )
        Text(stringResource(R.string.privacy_last_updated), style = UvType.lede, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))

        UvPanel {
            Spacer(Modifier.height(6.dp))
            Sections.forEach { (heading, body) -> Section(stringResource(heading), stringResource(body), open) }
            Section(stringResource(R.string.privacy_contact_heading), stringResource(R.string.privacy_contact_body, email), open, mailto = email, last = true)
        }
    }
}

@Composable
private fun Section(heading: String, body: String, open: (String) -> Unit, mailto: String? = null, last: Boolean = false) {
    val linkColor = LocalAccent.current.text
    Text(
        sectionText(heading, body, mailto, linkColor, open),
        style = UvType.body,
        color = UvColors.TextBody,
        modifier = Modifier.padding(bottom = if (last) 4.dp else 16.dp),
    )
}

/**
 * "**Heading:** body", with web addresses (and the contact email) as tappable links. Links go
 * through [open] so a missing browser or mail app shows a message instead of crashing.
 */
private fun sectionText(
    heading: String, body: String, mailto: String?, linkColor: Color, open: (String) -> Unit,
): AnnotatedString = buildAnnotatedString {
    val linkStyles = TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
    fun link(url: String) = LinkAnnotation.Clickable(url, linkStyles) { open(url) }
    withStyle(SpanStyle(color = UvColors.Text, fontWeight = FontWeight.SemiBold)) { append("$heading: ") }
    var last = 0
    UrlPattern.findAll(body).forEach { match ->
        append(body.substring(last, match.range.first))
        withLink(link(match.value)) { append(match.value) }
        last = match.range.last + 1
    }
    val rest = body.substring(last)
    if (mailto != null && rest.contains(mailto)) {
        val at = rest.indexOf(mailto)
        append(rest.substring(0, at))
        withLink(link("mailto:$mailto")) { append(mailto) }
        append(rest.substring(at + mailto.length))
    } else {
        append(rest)
    }
}
