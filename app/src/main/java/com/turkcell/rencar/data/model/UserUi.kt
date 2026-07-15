package com.turkcell.rencar.data.model

/**
 * Kullanıcı — UI/domain modeli (decisions.md → "Katman Derinliği" mapper katmanı).
 *
 * API DTO'su [com.turkcell.rencar.data.remote.dto.UserDto] UI'a/ViewModel'e doğrudan verilmez;
 * repository ayrı mapper katmanı ([com.turkcell.rencar.data.mapper.toUi]) ile bu modele çevirir.
 */
data class UserUi(
    val id: String,
    val email: String,
    val phone: String?,
    val fullName: String,
    val role: String,
)
