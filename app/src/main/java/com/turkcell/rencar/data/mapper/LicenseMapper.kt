package com.turkcell.rencar.data.mapper

import com.turkcell.rencar.data.model.LicenseVerificationStatus
import com.turkcell.rencar.data.remote.dto.LicenseStatusResponse

/**
 * Ehliyet durumu DTO → UI modeli (enum) dönüşümü (ayrı mapper katmanı; decisions.md → "Katman
 * Derinliği"). API'nin status string'i ([LicenseStatusResponse.status]) burada enum'a eşlenir;
 * bilinmeyen değer [LicenseVerificationStatus.UNKNOWN] olur ("Minimum Değişiklik İlkesi").
 */
fun LicenseStatusResponse.toVerificationStatus(): LicenseVerificationStatus = when (status) {
    "APPROVED" -> LicenseVerificationStatus.APPROVED
    "UNDER_REVIEW" -> LicenseVerificationStatus.UNDER_REVIEW
    "REJECTED" -> LicenseVerificationStatus.REJECTED
    "NOT_SUBMITTED" -> LicenseVerificationStatus.NOT_SUBMITTED
    else -> LicenseVerificationStatus.UNKNOWN
}
