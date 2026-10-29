package com.tubers.blacklight.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tubers.blacklight.domain.model.AppLanguage
import com.tubers.blacklight.domain.model.AppLanguages
import com.tubers.blacklight.ui.components.UvIcons
import com.tubers.blacklight.ui.components.readableWidth
import com.tubers.blacklight.ui.theme.LocalAccent
import com.tubers.blacklight.ui.theme.MonoFamily
import com.tubers.blacklight.ui.theme.UvColors
import java.util.Locale

/**
 * Language picker: shown once on first launch and from Settings › Language. [onBack] is null on
 * first launch, where there is nothing to go back to.
 */
@Composable
fun LanguageScreen(currentTag: String?, onBack: (() -> Unit)?, onContinue: (String) -> Unit) {
    val accent = LocalAccent.current
    var picked by rememberSaveable { mutableStateOf(currentTag ?: AppLanguages.preselect(Locale.getDefault().language)) }
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(query) { AppLanguages.search(query) }
    val chosen = AppLanguages.find(picked) ?: AppLanguages.all.first()

    Box(
        Modifier
            .fillMaxSize()
            .background(accent.ground)
            .drawBehind {
                val c = Offset(size.width * 0.95f, 0f)
                val r = size.minDimension * 0.75f
                drawCircle(
                    Brush.radialGradient(0f to accent.color.copy(alpha = 0.3f), 0.5f to accent.color.copy(alpha = 0.08f), 1f to Color.Transparent, center = c, radius = r),
                    radius = r, center = c,
                )
            }
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Column(Modifier.fillMaxSize().readableWidth().padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 20.dp)) {
            Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) RoundIconButton(UvIcons.Back, "Back", onBack)
            }

            Row(
                Modifier.padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(UvIcons.Globe, null, tint = accent.text, modifier = Modifier.size(14.dp))
                Text("LANGUAGE", fontFamily = MonoFamily, fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.6.sp, color = accent.text)
            }
            Text(
                "Choose your language", fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp,
                color = UvColors.Text, modifier = Modifier.padding(top = 8.dp).semantics { heading() },
            )
            Text(
                "You can change this anytime in Settings.", fontSize = 14.sp, lineHeight = 21.sp, color = UvColors.TextSecondary,
                modifier = Modifier.padding(top = 8.dp),
            )

            OutlinedTextField(
                value = query,
                onValueChange = { query = it.take(32) },
                placeholder = { Text("Search languages", color = UvColors.Placeholder, fontSize = 14.sp) },
                leadingIcon = { Icon(UvIcons.Search, null, tint = UvColors.TextSecondary, modifier = Modifier.size(18.dp)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                textStyle = androidx.compose.ui.text.TextStyle(color = UvColors.Text, fontSize = 14.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent.color,
                    unfocusedBorderColor = UvColors.Border,
                    focusedContainerColor = UvColors.Field,
                    unfocusedContainerColor = UvColors.Field,
                    cursorColor = accent.color,
                    focusedTextColor = UvColors.Text,
                    unfocusedTextColor = UvColors.Text,
                ),
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp).semantics { contentDescription = "Search languages" },
            )

            LazyColumn(
                Modifier.weight(1f).fillMaxWidth().padding(top = 14.dp).selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 12.dp),
            ) {
                items(results, key = { it.tag }) { lang ->
                    LanguageRow(lang, selected = lang.tag == picked, onClick = { picked = lang.tag })
                }
                if (results.isEmpty()) {
                    item {
                        Text(
                            "No languages match \"$query\"", fontSize = 13.sp, color = UvColors.TextSecondary,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        )
                    }
                }
            }

            HorizontalDivider(color = UvColors.Divider)
            ContinueButton(
                text = "Continue in ${chosen.nativeName}",
                enabled = true,
                onClick = { onContinue(chosen.tag) },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun LanguageRow(lang: AppLanguage, selected: Boolean, onClick: () -> Unit) {
    val accent = LocalAccent.current
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(shape)
            .background(if (selected) accent.color.copy(alpha = 0.12f) else UvColors.Surface)
            .border(1.5.dp, if (selected) accent.color else UvColors.BorderSoft, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (selected) accent.color else UvColors.SurfaceHigh),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                lang.tag.uppercase(Locale.ROOT), fontFamily = MonoFamily, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                color = if (selected) accent.onColor else UvColors.TextBody,
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(lang.nativeName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = UvColors.Text)
            Text(lang.englishName, fontSize = 12.sp, color = UvColors.TextSecondary)
        }
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .then(if (selected) Modifier.background(accent.color) else Modifier.border(1.5.dp, UvColors.Track, CircleShape)),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Icon(UvIcons.Check, null, tint = accent.onColor, modifier = Modifier.size(13.dp))
        }
    }
}
