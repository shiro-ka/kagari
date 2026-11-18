package dev.dimension.flare.data.datasource.microblog

public const val KAGARI_BLUESKY_MODERATION_DID: String = "did:plc:ar7c4by46qjdydhdevvrndac"

private const val KAGARI_REPORT_NEW = "tools.ozone.report.defs#"
private const val KAGARI_REPORT_OLD = "com.atproto.moderation.defs#"
private const val KAGARI_REPORT_POST_COLLECTION = "app.bsky.feed.post"

public enum class KagariReportCategory {
    Misleading,
    SexualAdultContent,
    HarassmentHate,
    ViolencePhysicalHarm,
    ChildSafety,
    SelfHarm,
    RuleBreaking,
    Other,
    ;

    public val reasons: List<KagariReportReason>
        get() = KagariReportReason.entries.filter { it.category == this }

    public val directReason: KagariReportReason?
        get() = if (this == Other) KagariReportReason.Other else null
}

public enum class KagariReportReason(
    key: String,
    legacyKey: String,
    public val category: KagariReportCategory,
    public val isOther: Boolean = false,
    public val officialOnly: Boolean = false,
) {
    MisleadingSpam("reasonMisleadingSpam", "reasonSpam", KagariReportCategory.Misleading),
    MisleadingScam("reasonMisleadingScam", "reasonMisleading", KagariReportCategory.Misleading),
    MisleadingBot("reasonMisleadingBot", "reasonMisleading", KagariReportCategory.Misleading),
    MisleadingImpersonation("reasonMisleadingImpersonation", "reasonMisleading", KagariReportCategory.Misleading),
    MisleadingElections("reasonMisleadingElections", "reasonMisleading", KagariReportCategory.Misleading),
    MisleadingOther("reasonMisleadingOther", "reasonMisleading", KagariReportCategory.Misleading, isOther = true),
    SexualUnlabeled("reasonSexualUnlabeled", "reasonSexual", KagariReportCategory.SexualAdultContent),
    SexualAbuseContent("reasonSexualAbuseContent", "reasonSexual", KagariReportCategory.SexualAdultContent),
    SexualNCII("reasonSexualNCII", "reasonSexual", KagariReportCategory.SexualAdultContent),
    SexualDeepfake("reasonSexualDeepfake", "reasonSexual", KagariReportCategory.SexualAdultContent),
    SexualAnimal("reasonSexualAnimal", "reasonSexual", KagariReportCategory.SexualAdultContent),
    SexualOther("reasonSexualOther", "reasonSexual", KagariReportCategory.SexualAdultContent, isOther = true),
    HarassmentTroll("reasonHarassmentTroll", "reasonRude", KagariReportCategory.HarassmentHate),
    HarassmentTargeted("reasonHarassmentTargeted", "reasonRude", KagariReportCategory.HarassmentHate),
    HarassmentHateSpeech("reasonHarassmentHateSpeech", "reasonRude", KagariReportCategory.HarassmentHate),
    HarassmentDoxxing("reasonHarassmentDoxxing", "reasonRude", KagariReportCategory.HarassmentHate),
    HarassmentOther("reasonHarassmentOther", "reasonRude", KagariReportCategory.HarassmentHate, isOther = true),
    ViolenceAnimal("reasonViolenceAnimal", "reasonViolation", KagariReportCategory.ViolencePhysicalHarm),
    ViolenceThreats("reasonViolenceThreats", "reasonViolation", KagariReportCategory.ViolencePhysicalHarm),
    ViolenceGraphicContent("reasonViolenceGraphicContent", "reasonViolation", KagariReportCategory.ViolencePhysicalHarm),
    ViolenceGlorification("reasonViolenceGlorification", "reasonViolation", KagariReportCategory.ViolencePhysicalHarm),
    ViolenceExtremistContent(
        "reasonViolenceExtremistContent",
        "reasonViolation",
        KagariReportCategory.ViolencePhysicalHarm,
        officialOnly = true,
    ),
    ViolenceTrafficking("reasonViolenceTrafficking", "reasonViolation", KagariReportCategory.ViolencePhysicalHarm),
    ViolenceOther("reasonViolenceOther", "reasonViolation", KagariReportCategory.ViolencePhysicalHarm, isOther = true),
    ChildSafetyCSAM("reasonChildSafetyCSAM", "reasonViolation", KagariReportCategory.ChildSafety, officialOnly = true),
    ChildSafetyGroom("reasonChildSafetyGroom", "reasonViolation", KagariReportCategory.ChildSafety, officialOnly = true),
    ChildSafetyPrivacy("reasonChildSafetyPrivacy", "reasonViolation", KagariReportCategory.ChildSafety),
    ChildSafetyHarassment("reasonChildSafetyHarassment", "reasonViolation", KagariReportCategory.ChildSafety),
    ChildSafetyOther(
        "reasonChildSafetyOther",
        "reasonViolation",
        KagariReportCategory.ChildSafety,
        isOther = true,
        officialOnly = true,
    ),
    SelfHarmContent("reasonSelfHarmContent", "reasonViolation", KagariReportCategory.SelfHarm),
    SelfHarmED("reasonSelfHarmED", "reasonViolation", KagariReportCategory.SelfHarm),
    SelfHarmStunts("reasonSelfHarmStunts", "reasonViolation", KagariReportCategory.SelfHarm),
    SelfHarmSubstances("reasonSelfHarmSubstances", "reasonViolation", KagariReportCategory.SelfHarm),
    SelfHarmOther("reasonSelfHarmOther", "reasonViolation", KagariReportCategory.SelfHarm, isOther = true),
    RuleSiteSecurity("reasonRuleSiteSecurity", "reasonViolation", KagariReportCategory.RuleBreaking),
    RuleProhibitedSales("reasonRuleProhibitedSales", "reasonViolation", KagariReportCategory.RuleBreaking),
    RuleBanEvasion("reasonRuleBanEvasion", "reasonViolation", KagariReportCategory.RuleBreaking),
    RuleOther("reasonRuleOther", "reasonViolation", KagariReportCategory.RuleBreaking, isOther = true),
    Other("reasonOther", "reasonOther", KagariReportCategory.Other, isOther = true),
    ;

    public val value: String = KAGARI_REPORT_NEW + key
    public val legacy: String = KAGARI_REPORT_OLD + legacyKey
}

public data class KagariReportLabeler(
    public val did: String,
    public val name: String,
    public val handle: String,
    public val avatar: String?,
    public val reasonTypes: List<String>?,
    public val subjectTypes: List<String>?,
    public val subjectCollections: List<String>?,
) {
    public val isOfficial: Boolean
        get() = did == KAGARI_BLUESKY_MODERATION_DID

    public fun supports(
        reason: KagariReportReason,
        isAccount: Boolean,
    ): Boolean {
        if (reason.officialOnly && !isOfficial) return false
        val subjectTypes = subjectTypes
        if (subjectTypes != null && (if (isAccount) "account" else "record") !in subjectTypes) return false
        val subjectCollections = subjectCollections
        if (!isAccount && subjectCollections != null && KAGARI_REPORT_POST_COLLECTION !in subjectCollections) return false
        val reasonTypes = reasonTypes
        if (reasonTypes != null && reason.value !in reasonTypes && reason.legacy !in reasonTypes) return false
        return true
    }

    public fun reasonTypeFor(reason: KagariReportReason): String {
        val reasonTypes = reasonTypes ?: return reason.value
        return if (reason.value !in reasonTypes && reason.legacy in reasonTypes) reason.legacy else reason.value
    }
}

public fun List<KagariReportLabeler>.kagariReportCandidates(
    reason: KagariReportReason,
    isAccount: Boolean,
): List<KagariReportLabeler> = filter { it.supports(reason, isAccount) }
