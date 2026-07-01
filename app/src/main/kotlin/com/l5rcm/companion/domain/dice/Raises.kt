package com.l5rcm.companion.domain.dice

/**
 * Raise / Target Number math (rulebook pp. 79, 82), pure and RNG-free so it can be tested and
 * reused independently of the roll itself.
 *
 * - A **called Raise** voluntarily raises the TN by +5. The number of called Raises per roll is
 *   capped at the character's Void Ring; an unskilled roll allows none.
 * - **Free Raises** (from mastery, techniques, kata…) do not count toward that cap and may instead
 *   *reduce* the TN by 5 each.
 * - If the raised TN isn't met, the roll fails even if the original TN was met.
 */
object Raises {

    /** Effective TN after applying called Raises (+5 each) and free Raises spent on lowering TN (−5 each). */
    fun effectiveTn(baseTn: Int, calledRaises: Int = 0, freeRaisesToTn: Int = 0): Int =
        baseTn + 5 * calledRaises - 5 * freeRaisesToTn

    /** Maximum called Raises allowed: the Void Ring, or 0 on an unskilled roll. */
    fun maxCalledRaises(voidRank: Int, unskilled: Boolean = false): Int =
        if (unskilled) 0 else voidRank.coerceAtLeast(0)

    /** A roll succeeds when its total meets or beats the (already-raised) effective TN. */
    fun isSuccess(total: Int, effectiveTn: Int): Boolean = total >= effectiveTn
}
