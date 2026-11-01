package com.example.ble.data

val LOVESPOUSE_PAYLOADS: List<BlePayload> = listOf(
    BlePayload(
        id = "lovespouse_enable",
        name = "Enable Toy",
        category = PayloadCategory.LOVESPOUSE,
        manufacturerData = ManufacturerData(
            companyId = 0x0000,
            data = byteArrayOf(0x55, 0x00, 0x01, 0x00, 0x00, 0xFF.toByte())
        ),
        description = "Activates nearby Lovespouse/Lovense toys"
    ),
    BlePayload(
        id = "lovespouse_stop",
        name = "Stop Toy (Denial of Pleasure)",
        category = PayloadCategory.LOVESPOUSE,
        manufacturerData = ManufacturerData(
            companyId = 0x0000,
            data = byteArrayOf(0x55, 0x00, 0x00, 0x00, 0x00, 0xFF.toByte())
        ),
        description = "Stops nearby Lovespouse/Lovense toys"
    ),
)
