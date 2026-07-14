package com.diprotec.inventario.data.remote.api

import com.diprotec.inventario.data.remote.dto.FinalizarInventarioRequest
import com.diprotec.inventario.data.remote.dto.FinalizarInventarioResponse
import com.diprotec.inventario.data.remote.dto.InventariosResponse
import com.diprotec.inventario.data.remote.dto.ProductosResponse
import com.diprotec.inventario.data.remote.dto.RegistroInventarioRequest
import com.diprotec.inventario.data.remote.dto.ReglasResponse
import com.diprotec.inventario.data.remote.dto.SendRegistroInventarioResponse
import com.diprotec.inventario.data.remote.dto.UbicacionesResponse
import com.diprotec.inventario.data.remote.dto.UnidadMedidasResponse
import com.diprotec.inventario.data.remote.dto.UsersResponse
import com.diprotec.inventario.data.remote.dto.VersionEntradaRequest
import com.diprotec.inventario.data.remote.dto.VersionResponse
import com.diprotec.inventario.data.remote.dto.ActivateDispositivoRequest
import com.diprotec.inventario.data.remote.dto.ActivateDispositivoResponse
import com.diprotec.inventario.data.remote.dto.LoginDispositivoResponse
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {

    @GET("api/website/v1/usuarios/{empresaRUT}/GetUsuarios")
    suspend fun getUsers(
        @Path("empresaRUT") empresaRUT: String,
        @Header("X-API-KEY") apiKey: String,
        @Header("Authorization") authorization: String,
        @Header("X-DEVICE-SESSION") deviceSession: String,
        @Header("X-DEVICE-SIGNATURE") deviceSignature: String,
        @Header("X-DEVICE-TIMESTAMP") deviceTimestamp: String
    ): Response<UsersResponse>

    @GET("api/website/v1/reglas/{empresaRUT}/GetReglas")
    suspend fun getReglas(
        @Path("empresaRUT") empresaRUT: String,
        @Header("X-API-KEY") apiKey: String,
        @Header("Authorization") authorization: String,
        @Header("X-DEVICE-SESSION") deviceSession: String,
        @Header("X-DEVICE-SIGNATURE") deviceSignature: String,
        @Header("X-DEVICE-TIMESTAMP") deviceTimestamp: String
    ): Response<ReglasResponse>

    @GET("api/website/v1/ubicaciones/{empresaRUT}/GetUbicaciones")
    suspend fun getUbicaciones(
        @Path("empresaRUT") empresaRUT: String,
        @Header("X-API-KEY") apiKey: String,
        @Header("Authorization") authorization: String,
        @Header("X-DEVICE-SESSION") deviceSession: String,
        @Header("X-DEVICE-SIGNATURE") deviceSignature: String,
        @Header("X-DEVICE-TIMESTAMP") deviceTimestamp: String
    ): Response<UbicacionesResponse>

    @POST("api/website/v1/dispositivos/{empresaRUT}/LoginDispositivo")
    suspend fun loginDispositivo(
        @Path("empresaRUT") empresaRUT: String,
        @Header("X-API-KEY") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body serialNumberPlain: RequestBody
    ): Response<LoginDispositivoResponse>

    @POST("api/website/v1/dispositivos/{empresaRUT}/ActivateDispositivo")
    suspend fun activateDispositivo(
        @Path("empresaRUT") empresaRUT: String,
        @Header("X-API-KEY") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body body: ActivateDispositivoRequest
    ): Response<ActivateDispositivoResponse>

    @POST("api/website/v1/versiones/{empresaRUT}/GetVersion")
    suspend fun getVersion(
        @Path("empresaRUT") empresaRUT: String,
        @Header("X-API-KEY") apiKey: String,
        @Header("Authorization") authorization: String,
        @Header("X-DEVICE-SESSION") deviceSession: String,
        @Header("X-DEVICE-SIGNATURE") deviceSignature: String,
        @Header("X-DEVICE-TIMESTAMP") deviceTimestamp: String,
        @Body body: VersionEntradaRequest
    ): Response<VersionResponse>

    @GET("api/website/v1/productos/{empresaRUT}/GetProductos")
    suspend fun getProductos(
        @Path("empresaRUT") empresaRUT: String,
        @Header("X-API-KEY") apiKey: String,
        @Header("Authorization") authorization: String,
        @Header("X-DEVICE-SESSION") deviceSession: String,
        @Header("X-DEVICE-SIGNATURE") deviceSignature: String,
        @Header("X-DEVICE-TIMESTAMP") deviceTimestamp: String
    ): Response<ProductosResponse>

    @GET("api/website/v1/unidadmedidas/{empresaRUT}/GetUnidadMedidas")
    suspend fun getUnidadMedidas(
        @Path("empresaRUT") empresaRUT: String,
        @Header("X-API-KEY") apiKey: String,
        @Header("Authorization") authorization: String,
        @Header("X-DEVICE-SESSION") deviceSession: String,
        @Header("X-DEVICE-SIGNATURE") deviceSignature: String,
        @Header("X-DEVICE-TIMESTAMP") deviceTimestamp: String
    ): Response<UnidadMedidasResponse>

    @GET("api/website/v1/inventarios/{empresaRUT}/GetInventarios")
    suspend fun getInventarios(
        @Path("empresaRUT") empresaRUT: String,
        @Header("X-API-KEY") apiKey: String,
        @Header("Authorization") authorization: String,
        @Header("X-DEVICE-SESSION") deviceSession: String,
        @Header("X-DEVICE-SIGNATURE") deviceSignature: String,
        @Header("X-DEVICE-TIMESTAMP") deviceTimestamp: String
    ): Response<InventariosResponse>

    @POST("api/website/v1/inventarios/{empresaRUT}/SendRegistroInventario")
    suspend fun sendRegistroInventario(
        @Path("empresaRUT") empresaRUT: String,
        @Header("X-API-KEY") apiKey: String,
        @Header("Authorization") authorization: String,
        @Header("X-DEVICE-SESSION") deviceSession: String,
        @Header("X-DEVICE-SIGNATURE") deviceSignature: String,
        @Header("X-DEVICE-TIMESTAMP") deviceTimestamp: String,
        @Body body: RegistroInventarioRequest
    ): Response<SendRegistroInventarioResponse>

    @POST("api/website/v1/inventarios/{empresaRUT}/FinishInventario")
    suspend fun finishInventario(
        @Path("empresaRUT") empresaRUT: String,
        @Header("X-API-KEY") apiKey: String,
        @Header("Authorization") authorization: String,
        @Header("X-DEVICE-SESSION") deviceSession: String,
        @Header("X-DEVICE-SIGNATURE") deviceSignature: String,
        @Header("X-DEVICE-TIMESTAMP") deviceTimestamp: String,
        @Body body: FinalizarInventarioRequest
    ): Response<FinalizarInventarioResponse>
}

