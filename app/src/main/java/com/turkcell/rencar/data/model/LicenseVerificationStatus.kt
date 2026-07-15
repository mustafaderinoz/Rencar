package com.turkcell.rencar.data.model

/**
 * Ehliyet doğrulama durumu — UI/domain modeli (decisions.md → "Katman Derinliği" mapper katmanı).
 *
 * API string'i ([com.turkcell.rencar.data.remote.dto.LicenseStatusResponse].status) ayrı mapper
 * katmanında ([com.turkcell.rencar.data.mapper.toVerificationStatus]) bu enum'a eşlenir; durum
 * alınamaz/bilinmezse [UNKNOWN]. (Önceden ui/profile içindeydi; repository de üretebilsin diye
 * data/model'e taşındı.)
 */
enum class LicenseVerificationStatus {
    APPROVED,
    UNDER_REVIEW,
    REJECTED,
    NOT_SUBMITTED,
    UNKNOWN,
}
