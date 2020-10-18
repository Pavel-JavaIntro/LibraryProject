package by.pavka.library.entity;

import by.pavka.library.entity.impl.Book;
import by.pavka.library.entity.impl.Edition;

import java.io.Serializable;
import java.util.Objects;

public class EditionInfo implements Serializable {
  private static final String AVAILABLE = "available";
  private static final String NOT_AVAILABLE = "not available";
  private Edition edition;
  private String authors;
  private Book book;

  public Edition getEdition() {
    return edition;
  }

  public void setEdition(Edition edition) {
    this.edition = edition;
  }

  public String getAuthors() {
    return authors;
  }

  public void setAuthors(String authors) {
    this.authors = authors;
  }

  public Book getBook() {
    return book;
  }

  public void setBook(Book book) {
    this.book = book;
  }

  public String getAvailability() {
    if (book == null) {
      return NOT_AVAILABLE;
    }
    return AVAILABLE;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    EditionInfo that = (EditionInfo) o;
    return edition.equals(that.edition)
        && Objects.equals(authors, that.authors)
        && Objects.equals(book, that.book);
  }

  @Override
  public int hashCode() {
    return Objects.hash(edition, authors, book);
  }

  @Override
  public String toString() {
    try {
      return String.format(
          "%s, %s, %s, AVAILABILITY: %s",
          edition.fieldForName("title").getValue(),
          authors,
          edition.fieldForName("year").getValue(),
          getAvailability());
    } catch (LibraryEntityException e) {
      return "";
    }
  }
}
