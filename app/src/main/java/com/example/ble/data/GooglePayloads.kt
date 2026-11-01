package com.example.ble.data

// Google Fast Pair service UUID
const val FAST_PAIR_UUID = "0000FE2C-0000-1000-8000-00805F9B34FB"

val GOOGLE_PAYLOADS: List<BlePayload> = listOf(
    BlePayload(
        id = "google_pixel_buds",
        name = "Pixel Buds",
        category = PayloadCategory.GOOGLE,
        serviceData = ServiceData(
            uuid = FAST_PAIR_UUID,
            data = byteArrayOf(0x82.toByte(), 0xBB.toByte(), 0xBD.toByte())
        )
    ),
    BlePayload(
        id = "google_pixel_buds_a",
        name = "Pixel Buds A-Series",
        category = PayloadCategory.GOOGLE,
        serviceData = ServiceData(
            uuid = FAST_PAIR_UUID,
            data = byteArrayOf(0x00, 0x00, 0x00)
        )
    ),
    BlePayload(
        id = "google_sony_wf1000xm5",
        name = "WF-1000XM5",
        category = PayloadCategory.GOOGLE,
        serviceData = ServiceData(
            uuid = FAST_PAIR_UUID,
            data = byteArrayOf(0x71, 0x86.toByte(), 0x07)
        )
    ),
    BlePayload(
        id = "google_sony_wh1000xm5",
        name = "WH-1000XM5",
        category = PayloadCategory.GOOGLE,
        serviceData = ServiceData(
            uuid = FAST_PAIR_UUID,
            data = byteArrayOf(0x8E.toByte(), 0x8D.toByte(), 0x6D)
        )
    ),
    BlePayload(
        id = "google_beats_fit_pro",
        name = "Beats Fit Pro",
        category = PayloadCategory.GOOGLE,
        serviceData = ServiceData(
            uuid = FAST_PAIR_UUID,
            data = byteArrayOf(0xB3.toByte(), 0x6F, 0x5E)
        )
    ),
    BlePayload(
        id = "google_razer_hammerhead",
        name = "Razer Hammerhead",
        category = PayloadCategory.GOOGLE,
        serviceData = ServiceData(
            uuid = FAST_PAIR_UUID,
            data = byteArrayOf(0x72, 0xEF.toByte(), 0x8D.toByte())
        )
    ),
)
