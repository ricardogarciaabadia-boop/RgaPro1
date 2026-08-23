package com.example.rgapro1

/**
 * Canonical product-specific extraction schema.
 * OCR text must be classified before fields are persisted.
 */
enum class PolicyProductType {
    VIDA,
    DECESOS,
    AHORRO,
    HOGAR,
    ACCIDENTES,
    DESCONOCIDO
}

data class PolicyCommonFields(
    val policyNumber: String? = null,
    val holderName: String? = null,
    val holderDni: String? = null,
    val issueDate: String? = null,
    val effectiveDate: String? = null,
    val expiryDate: String? = null,
    val renewalDate: String? = null
)

data class LifePolicyFields(
    val holderAddress: String? = null,
    val holderPhone: String? = null,
    val holderEmail: String? = null,
    val insuredCapitals: Map<String, String> = emptyMap()
)

data class FuneralInsured(
    val name: String,
    val birthDate: String? = null,
    val dni: String? = null,
    val capital: String? = null
)

data class FuneralPolicyFields(
    val insured: List<FuneralInsured> = emptyList()
)

data class SavingsPolicyFields(
    val contribution: String? = null,
    val guarantees: Map<String, String> = emptyMap()
)

data class StructuredPolicy(
    val productType: PolicyProductType,
    val common: PolicyCommonFields = PolicyCommonFields(),
    val life: LifePolicyFields? = null,
    val funeral: FuneralPolicyFields? = null,
    val savings: SavingsPolicyFields? = null,
    val rawOcrText: String? = null,
    val originalDocumentUri: String? = null
)

object PolicyProductClassifier {
    fun classify(text: String): PolicyProductType {
        val normalized = text.lowercase()
        return when {
            listOf("decesos", "funeral", "asistencia familiar").any(normalized::contains) -> PolicyProductType.DECESOS
            listOf("seguro de vida", "vida riesgo", "fallecimiento").any(normalized::contains) -> PolicyProductType.VIDA
            listOf("ahorro", "aportación", "aportacion", "prima periódica", "prima periodica").any(normalized::contains) -> PolicyProductType.AHORRO
            listOf("hogar", "vivienda", "continente", "contenido").any(normalized::contains) -> PolicyProductType.HOGAR
            listOf("accidentes", "accidente personal").any(normalized::contains) -> PolicyProductType.ACCIDENTES
            else -> PolicyProductType.DESCONOCIDO
        }
    }
}
