package com.bonushub.crdb.india.utils.ingenico

import com.ingenico.hdfcpayment.model.Track1
import com.ingenico.hdfcpayment.model.Track2


/**
 * Raw track data.
 *
 * The raw track data contains start/end sentinels and LRC
 * Track2 and track3 characters are converted to ascii by adding '0':
 * 0 -> '0' ... 9 -> '9', A -> ':', B -> ';', C -> '<', D -> '=', E -> '>', F -> '?'
 *
 * Destructuring removes sentinels and LRC (if present), and matches against expected format.
 *
 * @property track1 the content of track1 (format B)
 * @property track2 the content of track2
 * @property track3 the content of track3
 */
data class RawStripe(
    val rawTrack1: String? = null,
    val rawTrack2: String? = null,
    val rawTrack3: String? = null
) {
    val track1: Track1?
        get() {
            val groupValues = rawTrack1?.let {
                TRACK1_RE.matchEntire(it)?.groupValues
            }
            return groupValues?.let {
                Track1(
                    groupValues[1],
                    groupValues[2],
                    groupValues[3],
                    groupValues[4],
                    groupValues[5]
                )
            }
        }

    val track2: Track2?
        get() {
            val groupValues = rawTrack2?.let {
                TRACK2_RE.matchEntire(it)?.groupValues
            }
            return groupValues?.let {
                Track2(
                    groupValues[1],
                    groupValues[2],
                    groupValues[3],
                    groupValues[4]
                )
            }
        }

  /*  val track3: Track3?
        get() {
            val groupValues = rawTrack3?.let {
                TRACK3_RE.matchEntire(it)?.groupValues
            }
            return groupValues?.let {
                Track3(
                    groupValues.get(1)
                )
            }
        }*/

    companion object {
        val TRACK1_RE = Regex(
            """^(?:%B)?([0-9 ]{1,19})\^([^\^]{2,26})\^([0-9]{4}|\^)([0-9]{3}|\^)([^\?]+)(?:\?.?)?$""",
            RegexOption.DOT_MATCHES_ALL
        )
        val TRACK2_RE = Regex(
            """^;?([0-9]{1,19})\=([0-9]{4})([0-9]{3})([^\?]*)(\?.?)?$""",
            RegexOption.DOT_MATCHES_ALL
        )
        val TRACK3_RE = Regex(
            """^;?([^\?]+)(?:\?.?)?$""",
            RegexOption.DOT_MATCHES_ALL
        )
    }
}
/*
data class Track1(
    val pan: String,
    val name: String,
    val expirationDate: String,
    val serviceCode: String,
    val discretionaryData: String
) {
    val data: String
        get() = "$pan^$name^$expirationDate$serviceCode$discretionaryData"

    fun isExpired(yy: Int, mm: Int): Boolean =
        String.format("%02d%02d", yy % 100, mm) > expirationDate

}

data class Track2(
    val pan: String,
    val expirationDate: String,
    val serviceCode: String,
    val discretionaryData: String
) {
    val data: String
        get() = "$pan=$expirationDate$serviceCode$discretionaryData"

    fun isExpired(yy: Int, mm: Int): Boolean =
        String.format("%02d%02d", yy % 100, mm) > expirationDate
}

data class Track3(
    val data: String
)*/
