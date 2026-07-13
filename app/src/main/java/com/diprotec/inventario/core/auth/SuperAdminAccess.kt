package com.diprotec.inventario.core.auth

object SuperAdminAccess {

    private const val SUPER_ADMIN_RUT = "76001910-0"
    private const val SUPER_ADMIN_PASSWORD = "2R07v3dB"

    const val SUPER_ADMIN_NAME = "Superadmin"

    /**
     * Perfil interno solo para la sesión local.
     * No depende del perfil real del backend porque el superadmin
     * debe saltarse reglas asociadas a perfiles.
     */
    const val SUPER_ADMIN_PROFILE_ID = -999

    fun isSuperAdmin(rut: String?): Boolean {
        return normalizeRut(rut) == normalizeRut(SUPER_ADMIN_RUT)
    }

    fun isValidSuperAdminLogin(
        rut: String?,
        password: String?
    ): Boolean {
        return isSuperAdmin(rut) && password == SUPER_ADMIN_PASSWORD
    }

    fun getRut(): String {
        return SUPER_ADMIN_RUT
    }

    fun getName(): String {
        return SUPER_ADMIN_NAME
    }

    fun getProfileId(): Int {
        return SUPER_ADMIN_PROFILE_ID
    }

    private fun normalizeRut(value: String?): String {
        return value
            ?.trim()
            ?.uppercase()
            ?.replace(".", "")
            ?.replace(" ", "")
            ?: ""
    }
}