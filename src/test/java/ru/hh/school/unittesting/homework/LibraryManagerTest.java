package ru.hh.school.unittesting.homework;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LibraryManagerTest {
  @Mock
  private NotificationService notificationService;

  @Mock
  private UserService userService;

  @InjectMocks
  private LibraryManager libraryManager;

  @ParameterizedTest
  @CsvSource({
      "book1, 10, 10",
      "book2, -10, -10",
      "book3, 0, 0"
  })
  void testAddBookWhenBookIsNew(String bookId, int quantity, int expectedValue) {
    libraryManager.addBook(bookId, quantity);
    assertEquals(expectedValue, libraryManager.getAvailableCopies(bookId));
  }

  @ParameterizedTest
  @CsvSource({
      "book1, 10, 10, 20",
      "book2, -10, -10, -20",
      "book3, 5, 0, 5",
      "book4, 0, 7, 7",
      "book5, -5, 0, -5",
      "book6, 0, -7, -7",
      "book7, 0, 0, 0",
      "book8, -10, 10, 0",
      "book9, 10, -5, 5",
  })
  void testAddBookWhenBookAlreadyExists(String bookId, int existingQuantity, int quantity, int expectedValue) {
    libraryManager.addBook(bookId, existingQuantity);
    libraryManager.addBook(bookId, quantity);
    assertEquals(expectedValue, libraryManager.getAvailableCopies(bookId));
  }

  @Test
  void testBorrowBookWhenUserNotActive() {
    String userId = "user1";
    String bookId = "book1";

    when(userService.isUserActive(userId)).thenReturn(false);

    assertFalse(libraryManager.borrowBook(bookId, userId));
    assertEquals(0, libraryManager.getAvailableCopies(bookId));
    verify(notificationService, times(1)).notifyUser(userId, "Your account is not active.");
  }

  @Test
  void testBorrowBookWhenBookNotInInventory() {
    String userId = "user1";
    String bookId = "book1";

    when(userService.isUserActive(userId)).thenReturn(true);

    assertFalse(libraryManager.borrowBook(bookId, userId));
    assertEquals(0, libraryManager.getAvailableCopies(bookId));
  }

  @Test
  void testBorrowBookWhenNoCopiesAvailable() {
    String userId = "user1";
    String bookId = "book1";

    libraryManager.addBook(bookId, 0);

    when(userService.isUserActive(userId)).thenReturn(true);

    assertFalse(libraryManager.borrowBook(bookId, userId));
    assertEquals(0, libraryManager.getAvailableCopies(bookId));
  }

  @Test
  void testBorrowBookWhenSuccessful() {
    String userId = "user1";
    String bookId = "book1";

    libraryManager.addBook(bookId, 1);

    when(userService.isUserActive(userId)).thenReturn(true);

    assertTrue(libraryManager.borrowBook(bookId, userId));
    assertEquals(0, libraryManager.getAvailableCopies(bookId));
    verify(notificationService, times(1)).notifyUser(userId, "You have borrowed the book: " + bookId);
  }

  @Test
  void testReturnBookWhenBookNotBorrowed() {
    assertFalse(libraryManager.returnBook("book1", "user1"));
    assertEquals(0, libraryManager.getAvailableCopies("book1"));
  }

  @Test
  void testReturnBookWhenBookBorrowedByOtherUser() {
    String bookId = "book1";

    when(userService.isUserActive("user1")).thenReturn(true);

    libraryManager.addBook(bookId, 1);
    libraryManager.borrowBook(bookId, "user1");

    assertFalse(libraryManager.returnBook(bookId, "user2"));
    assertEquals(0, libraryManager.getAvailableCopies(bookId));
  }

  @Test
  void testReturnBookWhenSuccessful() {
    String userId = "user1";
    String bookId = "book1";

    when(userService.isUserActive(userId)).thenReturn(true);

    libraryManager.addBook(bookId, 1);
    libraryManager.borrowBook(bookId, userId);

    assertTrue(libraryManager.returnBook(bookId, userId));
    assertEquals(1, libraryManager.getAvailableCopies(bookId));
    verify(notificationService, times(1)).notifyUser(userId, "You have returned the book: " + bookId);
  }

  @Test
  void testGetAvailableCopiesWhenBookDoesNotExist() {
    assertEquals(0, libraryManager.getAvailableCopies("book1"));
  }

  @Test
  void testGetAvailableCopiesWhenBookExists() {
    libraryManager.addBook("book1", 10);
    assertEquals(10, libraryManager.getAvailableCopies("book1"));
  }

  @Test
  void testCalculateDynamicLateFeeWhenOverdueDaysNegative() {
    var exception = assertThrows(
        IllegalArgumentException.class,
        () -> libraryManager.calculateDynamicLateFee(-1, true, true)
    );

    assertEquals("Overdue days cannot be negative.", exception.getMessage());
  }

  @ParameterizedTest
  @CsvSource({
      "0, false, false, 0.00",
      "1, false, false, 0.50",
      "1, false, true, 0.40",
      "1, true, false, 0.75",
      "1, true, true, 0.60",
      "10, false, false, 5.00",
      "10, false, true, 4.00",
      "10, true, false, 7.50",
      "10, true, true, 6.00",
  })
  void testCalculateDynamicLateFee(int overdueDays, boolean isBestseller, boolean isPremiumMember, double expectedValue) {
    assertEquals(expectedValue, libraryManager.calculateDynamicLateFee(overdueDays, isBestseller, isPremiumMember));
  }

}
