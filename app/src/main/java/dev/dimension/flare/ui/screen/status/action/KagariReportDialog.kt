package dev.dimension.flare.ui.screen.status.action

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.dimension.flare.R
import dev.dimension.flare.data.datasource.microblog.KagariReportCategory
import dev.dimension.flare.data.datasource.microblog.KagariReportLabeler
import dev.dimension.flare.data.datasource.microblog.KagariReportReason
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.component.KagariActingAccountLine
import dev.dimension.flare.ui.model.UiState
import dev.dimension.flare.ui.model.takeSuccess
import dev.dimension.flare.ui.presenter.KagariReportPresenter
import dev.dimension.flare.ui.presenter.invoke
import dev.dimension.flare.ui.screen.KagariLoadingDialog
import dev.dimension.flare.ui.screen.kagariToast
import moe.tlaster.precompose.molecule.producePresenter

private const val KAGARI_REPORT_SUPPORT_URL = "https://bsky.social/about/support"
private const val KAGARI_REPORT_NCII_URL = "https://forms.bsky.app/f/ncii"
private const val KAGARI_REPORT_DETAILS_LIMIT = 300

private enum class KagariReportStep {
    Category,
    Reason,
    Labeler,
    Submit,
}

@Composable
internal fun KagariReportDialog(
    accountType: AccountType,
    onBack: () -> Unit,
    statusKey: MicroBlogKey? = null,
    userKey: MicroBlogKey? = null,
    fallback: (@Composable () -> Unit)? = null,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val state by producePresenter(key = "KagariReportPresenter_${accountType}_${statusKey}_$userKey") {
        remember(accountType, statusKey, userKey) {
            KagariReportPresenter(accountType = accountType, statusKey = statusKey, userKey = userKey)
        }.invoke()
    }
    if (!state.supported) {
        if (fallback != null) {
            fallback()
        } else {
            KagariLoadingDialog(onDismissRequest = onBack)
        }
        return
    }
    var step by remember { mutableStateOf(KagariReportStep.Category) }
    var category by remember { mutableStateOf<KagariReportCategory?>(null) }
    var reason by remember { mutableStateOf<KagariReportReason?>(null) }
    var labeler by remember { mutableStateOf<KagariReportLabeler?>(null) }
    var askNcii by remember { mutableStateOf(false) }
    var details by remember { mutableStateOf("") }
    var showDetails by remember { mutableStateOf(false) }
    val currentReason = reason
    val candidates = currentReason?.let { state.candidates(it) }
    val candidateList = candidates?.takeSuccess()
    LaunchedEffect(step, candidateList) {
        if (step == KagariReportStep.Labeler && candidateList != null && candidateList.size == 1) {
            labeler = candidateList.first()
            step = KagariReportStep.Submit
        }
    }
    val beforeLabeler =
        if (category?.directReason != null) KagariReportStep.Category else KagariReportStep.Reason
    val previous =
        when (step) {
            KagariReportStep.Category -> null
            KagariReportStep.Reason -> KagariReportStep.Category
            KagariReportStep.Labeler -> beforeLabeler
            KagariReportStep.Submit ->
                if (candidateList != null && candidateList.size == 1) beforeLabeler else KagariReportStep.Labeler
        }
    val goBack: () -> Unit = {
        if (previous == null) {
            onBack()
        } else {
            askNcii = false
            step = previous
        }
    }
    AlertDialog(
        onDismissRequest = onBack,
        title = {
            Text(
                text =
                    stringResource(
                        when (step) {
                            KagariReportStep.Category ->
                                if (state.isAccount) R.string.kagari_report_user_title else R.string.kagari_report_post_title
                            KagariReportStep.Reason -> R.string.kagari_report_select_reason
                            KagariReportStep.Labeler -> R.string.kagari_report_select_labeler
                            KagariReportStep.Submit -> R.string.kagari_report_submit
                        },
                    ),
            )
        },
        text = {
            BackHandler(enabled = previous != null) { goBack() }
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                when (step) {
                    KagariReportStep.Category -> {
                        Text(
                            text =
                                stringResource(
                                    if (state.isAccount) R.string.kagari_report_user_question else R.string.kagari_report_post_question,
                                ),
                        )
                        KagariActingAccountLine(accountType = accountType, modifier = Modifier.padding(vertical = 8.dp))
                        KagariReportCategory.entries.forEach { item ->
                            KagariReportCard(
                                title = stringResource(item.titleRes),
                                description = stringResource(item.descriptionRes),
                                onClick = {
                                    category = item
                                    labeler = null
                                    askNcii = false
                                    val direct = item.directReason
                                    if (direct != null) {
                                        reason = direct
                                        step = KagariReportStep.Labeler
                                    } else {
                                        reason = null
                                        step = KagariReportStep.Reason
                                    }
                                },
                            )
                        }
                        TextButton(onClick = { uriHandler.openUri(KAGARI_REPORT_SUPPORT_URL) }) {
                            Text(text = stringResource(R.string.kagari_report_legal_link))
                        }
                    }

                    KagariReportStep.Reason -> {
                        category?.reasons?.forEach { item ->
                            KagariReportCard(
                                title = stringResource(item.titleRes),
                                selected = reason == item,
                                onClick = {
                                    reason = item
                                    labeler = null
                                    if (item == KagariReportReason.SexualNCII) {
                                        askNcii = true
                                    } else {
                                        askNcii = false
                                        step = KagariReportStep.Labeler
                                    }
                                },
                            )
                        }
                        if (askNcii) {
                            Text(
                                text = stringResource(R.string.kagari_report_ncii_question),
                                modifier = Modifier.padding(top = 12.dp),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                            ) {
                                TextButton(
                                    onClick = {
                                        askNcii = false
                                        step = KagariReportStep.Labeler
                                    },
                                ) {
                                    Text(text = stringResource(R.string.kagari_report_no))
                                }
                                Button(
                                    onClick = {
                                        uriHandler.openUri(KAGARI_REPORT_NCII_URL)
                                        onBack()
                                    },
                                ) {
                                    Text(text = stringResource(R.string.kagari_report_yes))
                                }
                            }
                        }
                    }

                    KagariReportStep.Labeler -> {
                        when (candidates) {
                            is UiState.Success -> {
                                if (candidates.data.isEmpty()) {
                                    Text(text = stringResource(R.string.kagari_report_no_labeler))
                                } else {
                                    candidates.data.forEach { item ->
                                        KagariReportCard(
                                            title = item.name,
                                            description = stringResource(R.string.kagari_report_labeler_by, "@${item.handle}"),
                                            selected = labeler?.did == item.did,
                                            onClick = {
                                                labeler = item
                                                step = KagariReportStep.Submit
                                            },
                                        )
                                    }
                                }
                            }

                            is UiState.Error -> Text(text = stringResource(R.string.kagari_report_load_failed))

                            else ->
                                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                        }
                    }

                    KagariReportStep.Submit -> {
                        Text(text = stringResource(R.string.kagari_report_sent_to, labeler?.name.orEmpty()))
                        if (currentReason?.isOther == true || showDetails) {
                            OutlinedTextField(
                                value = details,
                                onValueChange = { details = it },
                                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                                label = { Text(text = stringResource(R.string.kagari_report_details_label)) },
                                isError = details.length > KAGARI_REPORT_DETAILS_LIMIT,
                                supportingText = { Text(text = "${details.length}/$KAGARI_REPORT_DETAILS_LIMIT") },
                                minLines = 3,
                            )
                        } else {
                            TextButton(onClick = { showDetails = true }) {
                                Text(text = stringResource(R.string.kagari_report_add_details))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (step == KagariReportStep.Submit) {
                val selectedLabeler = labeler
                TextButton(
                    enabled =
                        !state.sending &&
                            selectedLabeler != null &&
                            currentReason != null &&
                            details.length <= KAGARI_REPORT_DETAILS_LIMIT,
                    onClick = {
                        if (selectedLabeler != null && currentReason != null) {
                            state.send(selectedLabeler, currentReason, details) { ok ->
                                if (ok) {
                                    onBack()
                                } else {
                                    kagariToast(context, R.string.kagari_report_failed)
                                }
                            }
                        }
                    },
                ) {
                    Text(text = stringResource(R.string.kagari_report_submit))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = goBack) {
                Text(
                    text =
                        if (previous == null) {
                            stringResource(android.R.string.cancel)
                        } else {
                            stringResource(R.string.kagari_report_back)
                        },
                )
            }
        },
    )
}

@Composable
private fun KagariReportCard(
    title: String,
    onClick: () -> Unit,
    description: String? = null,
    selected: Boolean = false,
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors =
            if (selected) {
                CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            } else {
                CardDefaults.outlinedCardColors()
            },
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private val KagariReportCategory.titleRes: Int
    get() =
        when (this) {
            KagariReportCategory.Misleading -> R.string.kagari_report_category_misleading_title
            KagariReportCategory.SexualAdultContent -> R.string.kagari_report_category_sexual_adult_content_title
            KagariReportCategory.HarassmentHate -> R.string.kagari_report_category_harassment_hate_title
            KagariReportCategory.ViolencePhysicalHarm -> R.string.kagari_report_category_violence_physical_harm_title
            KagariReportCategory.ChildSafety -> R.string.kagari_report_category_child_safety_title
            KagariReportCategory.SelfHarm -> R.string.kagari_report_category_self_harm_title
            KagariReportCategory.RuleBreaking -> R.string.kagari_report_category_rule_breaking_title
            KagariReportCategory.Other -> R.string.kagari_report_category_other_title
        }

private val KagariReportCategory.descriptionRes: Int
    get() =
        when (this) {
            KagariReportCategory.Misleading -> R.string.kagari_report_category_misleading_description
            KagariReportCategory.SexualAdultContent -> R.string.kagari_report_category_sexual_adult_content_description
            KagariReportCategory.HarassmentHate -> R.string.kagari_report_category_harassment_hate_description
            KagariReportCategory.ViolencePhysicalHarm -> R.string.kagari_report_category_violence_physical_harm_description
            KagariReportCategory.ChildSafety -> R.string.kagari_report_category_child_safety_description
            KagariReportCategory.SelfHarm -> R.string.kagari_report_category_self_harm_description
            KagariReportCategory.RuleBreaking -> R.string.kagari_report_category_rule_breaking_description
            KagariReportCategory.Other -> R.string.kagari_report_category_other_description
        }

private val KagariReportReason.titleRes: Int
    get() =
        when (this) {
            KagariReportReason.MisleadingSpam -> R.string.kagari_report_reason_misleading_spam
            KagariReportReason.MisleadingScam -> R.string.kagari_report_reason_misleading_scam
            KagariReportReason.MisleadingBot -> R.string.kagari_report_reason_misleading_bot
            KagariReportReason.MisleadingImpersonation -> R.string.kagari_report_reason_misleading_impersonation
            KagariReportReason.MisleadingElections -> R.string.kagari_report_reason_misleading_elections
            KagariReportReason.MisleadingOther -> R.string.kagari_report_reason_misleading_other
            KagariReportReason.SexualUnlabeled -> R.string.kagari_report_reason_sexual_unlabeled
            KagariReportReason.SexualAbuseContent -> R.string.kagari_report_reason_sexual_abuse_content
            KagariReportReason.SexualNCII -> R.string.kagari_report_reason_sexual_ncii
            KagariReportReason.SexualDeepfake -> R.string.kagari_report_reason_sexual_deepfake
            KagariReportReason.SexualAnimal -> R.string.kagari_report_reason_sexual_animal
            KagariReportReason.SexualOther -> R.string.kagari_report_reason_sexual_other
            KagariReportReason.HarassmentTroll -> R.string.kagari_report_reason_harassment_troll
            KagariReportReason.HarassmentTargeted -> R.string.kagari_report_reason_harassment_targeted
            KagariReportReason.HarassmentHateSpeech -> R.string.kagari_report_reason_harassment_hate_speech
            KagariReportReason.HarassmentDoxxing -> R.string.kagari_report_reason_harassment_doxxing
            KagariReportReason.HarassmentOther -> R.string.kagari_report_reason_harassment_other
            KagariReportReason.ViolenceAnimal -> R.string.kagari_report_reason_violence_animal
            KagariReportReason.ViolenceThreats -> R.string.kagari_report_reason_violence_threats
            KagariReportReason.ViolenceGraphicContent -> R.string.kagari_report_reason_violence_graphic_content
            KagariReportReason.ViolenceGlorification -> R.string.kagari_report_reason_violence_glorification
            KagariReportReason.ViolenceExtremistContent -> R.string.kagari_report_reason_violence_extremist_content
            KagariReportReason.ViolenceTrafficking -> R.string.kagari_report_reason_violence_trafficking
            KagariReportReason.ViolenceOther -> R.string.kagari_report_reason_violence_other
            KagariReportReason.ChildSafetyCSAM -> R.string.kagari_report_reason_child_safety_csam
            KagariReportReason.ChildSafetyGroom -> R.string.kagari_report_reason_child_safety_groom
            KagariReportReason.ChildSafetyPrivacy -> R.string.kagari_report_reason_child_safety_privacy
            KagariReportReason.ChildSafetyHarassment -> R.string.kagari_report_reason_child_safety_harassment
            KagariReportReason.ChildSafetyOther -> R.string.kagari_report_reason_child_safety_other
            KagariReportReason.SelfHarmContent -> R.string.kagari_report_reason_self_harm_content
            KagariReportReason.SelfHarmED -> R.string.kagari_report_reason_self_harm_ed
            KagariReportReason.SelfHarmStunts -> R.string.kagari_report_reason_self_harm_stunts
            KagariReportReason.SelfHarmSubstances -> R.string.kagari_report_reason_self_harm_substances
            KagariReportReason.SelfHarmOther -> R.string.kagari_report_reason_self_harm_other
            KagariReportReason.RuleSiteSecurity -> R.string.kagari_report_reason_rule_site_security
            KagariReportReason.RuleProhibitedSales -> R.string.kagari_report_reason_rule_prohibited_sales
            KagariReportReason.RuleBanEvasion -> R.string.kagari_report_reason_rule_ban_evasion
            KagariReportReason.RuleOther -> R.string.kagari_report_reason_rule_other
            KagariReportReason.Other -> R.string.kagari_report_reason_other
        }
