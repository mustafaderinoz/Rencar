package com.turkcell.rencar.data.mapper

import com.turkcell.rencar.data.model.UserUi
import com.turkcell.rencar.data.remote.dto.UserDto

/**
 * Kullanıcı DTO → UI modeli dönüşümü (ayrı mapper katmanı; decisions.md → "Katman Derinliği").
 * API şema değişiklikleri yalnızca burada karşılanır ("Minimum Değişiklik İlkesi").
 */
fun UserDto.toUi(): UserUi = UserUi(
    id = id,
    email = email,
    phone = phone,
    fullName = fullName,
    role = role,
)
