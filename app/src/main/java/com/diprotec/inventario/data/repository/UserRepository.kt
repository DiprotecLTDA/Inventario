package com.diprotec.inventario.data.repository

import com.diprotec.inventario.data.local.entity.UserEntity
import com.diprotec.inventario.data.remote.dto.UserDto
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun fetchRemoteUsers(): List<UserDto>
    suspend fun replaceAllUsers(list: List<UserEntity>)
    fun observeUsers(): Flow<List<UserEntity>>
}
