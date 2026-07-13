package com.diprotec.inventario.service

import com.diprotec.inventario.core.auth.SuperAdminAccess
import com.diprotec.inventario.core.crypto.PasswordVerifier
import com.diprotec.inventario.data.local.dao.UserDao
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class AuthService @Inject constructor(
    private val userDao: UserDao
) {
    suspend fun login(username: String, passwordPlain: String): AuthResult =
        withContext(Dispatchers.IO) {
            val rut = username.trim()

            if (SuperAdminAccess.isSuperAdmin(rut)) {
                return@withContext if (
                    SuperAdminAccess.isValidSuperAdminLogin(
                        rut = rut,
                        password = passwordPlain
                    )
                ) {
                    AuthResult.Success(
                        perfilId = SuperAdminAccess.getProfileId()
                    )
                } else {
                    AuthResult.Invalid("Usuario o contraseña incorrectos.")
                }
            }

            val user = userDao.findByRut(rut)
                ?: return@withContext AuthResult.Invalid("Usuario no encontrado. ¿Sincronizaste?")

            if (user.vigente) {
                return@withContext AuthResult.Invalid("Usuario inhabilitado.")
            }

            val hash = user.passwordHash
                ?: return@withContext AuthResult.Invalid("Usuario sin credenciales.")

            val salt = user.passwordSalt
                ?: return@withContext AuthResult.Invalid("Usuario sin credenciales.")

            val algo = user.passwordAlgoritmo
                ?: return@withContext AuthResult.Invalid("Usuario sin credenciales.")

            return@withContext try {
                val ok = PasswordVerifier.verify(
                    passwordPlain,
                    hash,
                    salt,
                    algo
                )

                if (ok) {
                    val perfilId = user.perfilId ?: 0
                    AuthResult.Success(perfilId = perfilId)
                } else {
                    AuthResult.Invalid("Usuario o contraseña incorrectos.")
                }
            } catch (e: UnsupportedOperationException) {
                AuthResult.Invalid(e.message ?: "Algoritmo no soportado")
            }
        }
}

sealed class AuthResult {
    data class Success(val perfilId: Int) : AuthResult()
    data class Invalid(val reason: String) : AuthResult()
}