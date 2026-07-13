package com.diprotec.inventario.data.local.inventory

data class InventoryGroupedRow(
    val barcode: String,
    val description: String,
    val unitMeasure: String,
    val ubicacionId: String,
    val ubicacionNombre: String,
    val totalQuantity: Double,
    val totalRows: Int
)