package com.example.ble.data

val MICROSOFT_PAYLOADS: List<BlePayload> = listOf(
    BlePayload(
        id = "microsoft_swift_pair",
        name = "Swift Pair",
        category = PayloadCategory.MICROSOFT,
        manufacturerData = ManufacturerData(
            companyId = 0x0006,
            data = byteArrayOf(0x03, 0x00, 0x80.toByte())
        ),
        includeDeviceName = true,
        description = "Triggers Windows Swift Pair popup"
    ),
)
