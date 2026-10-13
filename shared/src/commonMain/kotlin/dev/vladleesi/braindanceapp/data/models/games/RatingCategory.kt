// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.data.models.games

import kotlinx.serialization.Serializable

@Serializable
enum class RatingCategory(
    val id: Int,
) {
    RP(1),
    EC(2),
    E(3),
    E10_PLUS(4),
    T(5),
    M(6),
    AO(7),
    PEGI_3(8),
    PEGI_7(9),
    PEGI_12(10),
    PEGI_16(11),
    PEGI_18(12),
    CERO_A(13),
    CERO_B(14),
    CERO_C(15),
    CERO_D(16),
    CERO_Z(17),
    USK_0(18),
    USK_6(19),
    USK_12(20),
    USK_16(21),
    USK_18(22),
    CLASS_IND_ALL(23),
    CLASS_IND_12_PLUS(24),
    CLASS_IND_15_PLUS(25),
    GRAC_19_PLUS(26),
    TESTING(27),
    ACB_L(28),
    ACB_10(29),
    ACB_12(30),
    ACB_14(31),
    ACB_16(32),
    ACB_18(33),
    ACB_G(34),
    ACB_PG(35),
    ACB_M(36),
    ACB_MA15_PLUS(37),
    ACB_R18_PLUS(38),
    ACB_RC(39),
}
