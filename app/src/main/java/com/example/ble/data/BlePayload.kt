package com.example.ble.data

/**
 * Represents a BLE advertisement payload with metadata.
 */
data class BlePayload(
    val id: String,
    val name: String,
    val category: PayloadCategory,
    val manufacturerData: ManufacturerData? = null,
    val serviceData: ServiceData? = null,
    val scanResponseData: ByteArray? = null,
    val includeDeviceName: Boolean = false,
    val description: String = ""
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BlePayload) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

data class ManufacturerData(
    val companyId: Int,
    val data: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ManufacturerData) return false
        return companyId == other.companyId && data.contentEquals(other.data)
    }
    override fun hashCode(): Int = 31 * companyId + data.contentHashCode()
}

data class ServiceData(
    val uuid: String,
    val data: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ServiceData) return false
        return uuid == other.uuid && data.contentEquals(other.data)
    }
    override fun hashCode(): Int = 31 * uuid.hashCode() + data.contentHashCode()
}

enum class PayloadCategory(val displayName: String) {
    APPLE("Apple"),
    MICROSOFT("Microsoft"),
    SAMSUNG("Samsung"),
    GOOGLE("Google Fast Pair"),
    LOVESPOUSE("Lovespouse"),
    IOS_CRASH("iOS Crash")
}
