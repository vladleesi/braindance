// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.details

import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryStatusTest {
    @Test
    fun theCollectionActionAddsAndRemovesTheGame() {
        assertEquals(LibraryStatus.Collection, LibraryStatus.None.after(LibraryAction.Collection))
        assertEquals(LibraryStatus.None, LibraryStatus.Collection.after(LibraryAction.Collection))
    }

    @Test
    fun addingAWishlistedGameToTheCollectionTakesItOffTheWishlist() {
        assertEquals(LibraryStatus.Collection, LibraryStatus.Wishlist.after(LibraryAction.Collection))
    }

    @Test
    fun theWishlistActionTogglesTheWishlist() {
        assertEquals(LibraryStatus.Wishlist, LibraryStatus.None.after(LibraryAction.Wishlist))
        assertEquals(LibraryStatus.None, LibraryStatus.Wishlist.after(LibraryAction.Wishlist))
    }

    @Test
    fun aGameInTheCollectionCannotBeWishlisted() {
        assertEquals(LibraryStatus.Collection, LibraryStatus.Collection.after(LibraryAction.Wishlist))
    }
}
