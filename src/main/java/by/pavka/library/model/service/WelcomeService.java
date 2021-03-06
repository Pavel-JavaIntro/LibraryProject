package by.pavka.library.model.service;

import by.pavka.library.BookOrder;
import by.pavka.library.entity.EditionInfo;
import by.pavka.library.entity.LibraryEntityException;
import by.pavka.library.entity.SimpleListEntity;
import by.pavka.library.entity.criteria.Criteria;
import by.pavka.library.entity.criteria.EntityField;
import by.pavka.library.entity.impl.Author;
import by.pavka.library.entity.impl.Book;
import by.pavka.library.entity.impl.Edition;
import by.pavka.library.entity.impl.User;
import by.pavka.library.model.dao.DaoException;
import by.pavka.library.model.dao.LibraryDao;
import by.pavka.library.model.dao.ManyToManyDao;
import by.pavka.library.model.dao.impl.LibraryDaoFactory;
import by.pavka.library.model.mapper.ConstantManager;
import by.pavka.library.model.mapper.TableEntityMapper;

import java.util.*;

public class WelcomeService implements WelcomeServiceInterface {
  private static WelcomeService instance = new WelcomeService();

  private WelcomeService() {}

  public static WelcomeService getInstance() {
    return instance;
  }

  @Override
  public <T extends SimpleListEntity> void initConstants(
      Map<Integer, String> constants, TableEntityMapper constant) throws ServiceException {
    List<T> list = null;
    LibraryDao<T> dao = null;
    try {
      dao = LibraryDaoFactory.getInstance().obtainDao(constant);
      list = dao.read();
      // dao.close();
    } catch (DaoException e) {
      throw new ServiceException("Cannot initialize constants", e);
    } finally {
      if (dao != null) {
        dao.close();
      }
    }
    for (T entity : list) {
      constants.put(entity.getId(), entity.getDescription());
    }
  }

  @Override
  public int countBooks() throws ServiceException {
    try (LibraryDao<Book> dao = LibraryDaoFactory.getInstance().obtainDao(TableEntityMapper.BOOK)) {
      return dao.read().size();
    } catch (DaoException e) {
      throw new ServiceException("Cannot count the books", e);
    }
  }

  @Override
  public int countUsers() throws ServiceException {
    try (LibraryDao<User> dao = LibraryDaoFactory.getInstance().obtainDao(TableEntityMapper.USER)) {
      return dao.read().size() - 1;
    } catch (DaoException e) {
      throw new ServiceException("Cannot count the books", e);
    }
  }

  @Override
  public User auth(String surname, String name, String password) throws ServiceException {
    int hashPass = password.hashCode();
    try (LibraryDao<User> dao = LibraryDaoFactory.getInstance().obtainDao(TableEntityMapper.USER)) {
      Criteria criteria = new Criteria();
      EntityField<String> surnameField = new EntityField<>("surname");
      surnameField.setValue(surname);
      EntityField<String> nameField = new EntityField<>("name");
      nameField.setValue(name);
      EntityField<Integer> passField = new EntityField<>("password");
      passField.setValue(hashPass);
      criteria.addConstraints(surnameField, nameField, passField);
      List<User> users = dao.read(criteria, true);
      if (users.size() > 0) {
        return users.get(0);
      } else {
        return null;
      }
    } catch (DaoException e) {
      throw new ServiceException("Cannot authorize the user", e);
    }
  }

  @Override
  public List<Book> findBooksByEditionCode(String code) throws ServiceException {
    List<Book> books;
    try (LibraryDao<Edition> editionDao =
        LibraryDaoFactory.getInstance().obtainDao(TableEntityMapper.EDITION)) {
      EntityField<String> field = new EntityField<>("standard_number");
      field.setValue(code);
      Criteria criteria = new Criteria();
      criteria.addConstraint(field);
      List<Edition> editions = editionDao.read(criteria, true);
      int editionId = 0;
      if (!editions.isEmpty()) {
        editionId = editions.get(0).getId();
      }
      books = findBooksByEdition(editionId);
    } catch (DaoException e) {
      throw new ServiceException("Cannot find editions", e);
    }
    return books;
  }

  @Override
  public List<Book> findBooksByEdition(int id) throws ServiceException {
    List<Book> result = new ArrayList<>();
    try (LibraryDao<Book> bookDao =
        LibraryDaoFactory.getInstance().obtainDao(TableEntityMapper.BOOK)) {
      Criteria criteria = new Criteria();
      EntityField<Integer> edId = new EntityField<>("editionId");
      edId.setValue(id);
      criteria.addConstraint(edId);
      result.addAll(bookDao.read(criteria, true));
    } catch (DaoException e) {
      throw new ServiceException("Cannot find books", e);
    }
    return result;
  }

  @Override
  public List<Edition> findEditions(String title, String author) throws ServiceException {
    try (ManyToManyDao<Edition, Author> editionDao =
        LibraryDaoFactory.getInstance().obtainManyToManyDao()) {
      if (title.isEmpty() && author.isEmpty()) {
        return editionDao.read();
      }

      List<Edition> titleEditions = null;
      List<Edition> authorEditions = null;

      if (!title.isEmpty()) {
        Criteria criteriaT = new Criteria();
        EntityField<String> titleField = new EntityField<>("title");
        titleField.setValue(title);
        criteriaT.addConstraint(titleField);
        titleEditions = editionDao.read(criteriaT, false);
        if (titleEditions.isEmpty()) {
          return new ArrayList<>();
        }
      }

      List<Author> authorList = null;
      if (!author.isEmpty()) {
        try (LibraryDao<Author> authorDao =
            LibraryDaoFactory.getInstance().obtainDao(TableEntityMapper.AUTHOR)) {
          Criteria criteriaA = new Criteria();
          EntityField<String> authorField = new EntityField<>("surname");
          authorField.setValue(author);
          criteriaA.addConstraint(authorField);
          authorList = authorDao.read(criteriaA, false);
          if (authorList.isEmpty()) {
            return new ArrayList<>();
          }
          Set<Integer> editionIds = new HashSet<>();
          for (Author a : authorList) {
            editionIds.addAll(editionDao.getFirst(a.getId()));
          }
          if (editionIds.isEmpty()) {
            return new ArrayList<>();
          }
          authorEditions = new ArrayList<>();
          for (int i : editionIds) {
            authorEditions.add(editionDao.get(i));
          }
          if (authorEditions.isEmpty()) {
            return new ArrayList<>();
          }
        }
      }

      List<Edition> finalEditions = null;
      if (titleEditions == null) {
        finalEditions = authorEditions;
      }
      if (authorEditions == null) {
        finalEditions = titleEditions;
      }
      if (titleEditions != null && authorEditions != null) {
        finalEditions = new ArrayList<>();
        for (Edition edition : titleEditions) {
          if (authorEditions.contains(edition)) {
            finalEditions.add(edition);
          }
        }
        if (finalEditions.isEmpty()) {
          return new ArrayList<>();
        }
      }
      return finalEditions;
    } catch (DaoException e) {
      throw new ServiceException("Cannot find books", e);
    }
  }

  @Override
  public Book findFreeBookByEdition(int id) throws ServiceException {
    Book book = null;
    try {
      List<Book> result = findBooksByEdition(id);
      for (Book b : result) {
        System.out.println(b.fieldForName(Book.RESERVED).getValue());
        if (!b.fieldForName(Book.LOCATION_ID)
                .getValue()
                .equals(ConstantManager.LOCATION_DECOMMISSIONED)
            && !b.fieldForName(Book.LOCATION_ID).getValue().equals(ConstantManager.LOCATION_ON_HAND)
            && !b.fieldForName(Book.RESERVED).getValue().equals(ConstantManager.RESERVED)
            && !b.fieldForName(Book.RESERVED).getValue().equals(ConstantManager.PREPARED)) {
          book = b;
          break;
        }
      }
    } catch (LibraryEntityException e) {
      throw new ServiceException("Cannot find books", e);
    }
    return book;
  }

  @Override
  public void bindAuthors(EditionInfo info) throws ServiceException {
    try (ManyToManyDao<Edition, Author> editionDao =
            LibraryDaoFactory.getInstance().obtainManyToManyDao();
        LibraryDao<Author> authorDao =
            LibraryDaoFactory.getInstance().obtainDao(TableEntityMapper.AUTHOR)) {
      Set<Author> authors = new HashSet<>();
      Set<Integer> authorIds = editionDao.getSecond(info.getEdition().getId());
      for (int id : authorIds) {
        authors.add(authorDao.get(id));
      }
      StringBuilder stringBuilder = new StringBuilder();
      for (Author a : authors) {
        stringBuilder.append(a.fieldForName("surname").getValue()).append(" ");
      }
      info.setAuthors(stringBuilder.toString());

    } catch (DaoException | LibraryEntityException e) {
      throw new ServiceException("Cannot find relevant books", e);
    }
  }

  @Override
  public void bindBookAndLocation(EditionInfo info) throws ServiceException {
    try {
      Book book = findFreeBookByEdition(info.getEdition().getId());
      info.setBook(book);
      if (book != null) {
        int locationId = (int) book.fieldForName("locationId").getValue();
        info.setLocationId(locationId);
      }
    } catch (ServiceException | LibraryEntityException e) {
      throw new ServiceException("Cannot find relevant books", e);
    }
  }

  @Override
  public void addCode(String code) throws ServiceException {
    try (LibraryDao<Edition> editionLibraryDao =
        LibraryDaoFactory.getInstance().obtainDao(TableEntityMapper.EDITION)) {
      Edition edition = new Edition();
      edition.setValue("standardNumber", code);
      editionLibraryDao.add(edition);
    } catch (DaoException e) {
      throw new ServiceException("Cannot add an edition code", e);
    }
  }

  @Override
  public int editionIdByCode(String code) throws ServiceException {
    int editionId = 0;
    try (LibraryDao<Edition> editionDao =
        LibraryDaoFactory.getInstance().obtainDao(TableEntityMapper.EDITION)) {
      EntityField<String> field = new EntityField<>("standard_number");
      field.setValue(code);
      Criteria criteria = new Criteria();
      criteria.addConstraint(field);
      List<Edition> editions = editionDao.read(criteria, true);
      if (!editions.isEmpty()) {
        editionId = editions.get(0).getId();
      }
    } catch (DaoException e) {
      throw new ServiceException("Cannot identify edition standard number", e);
    }
    return editionId;
  }

  @Override
  public void addBook(Book book) throws ServiceException {
    try (LibraryDao<Book> bookDao =
        LibraryDaoFactory.getInstance().obtainDao(TableEntityMapper.BOOK)) {
      bookDao.add(book);
    } catch (DaoException e) {
      throw new ServiceException("Cannot add a book", e);
    }
  }

  @Override
  public int addEdition(Edition edition) throws ServiceException {
    try (LibraryDao<Edition> editionDao =
        LibraryDaoFactory.getInstance().obtainDao(TableEntityMapper.EDITION)) {
      return editionDao.add(edition);
    } catch (DaoException e) {
      throw new ServiceException("Cannot add an edition code", e);
    }
  }

  @Override
  public int addAuthor(Author author) throws ServiceException {
    try (LibraryDao<Author> authorDao =
        LibraryDaoFactory.getInstance().obtainDao(TableEntityMapper.AUTHOR)) {
      return authorDao.add(author);
    } catch (DaoException e) {
      throw new ServiceException("Cannot add an author", e);
    }
  }

  @Override
  public void bindEditionAndAuthors(int editionId, int[] authorsId) throws ServiceException {
    try (ManyToManyDao<Edition, Author> dao =
        LibraryDaoFactory.getInstance().obtainManyToManyDao()) {
      for (int id : authorsId) {
        if (id != 0) {
          dao.bind(editionId, id);
        }
      }
    } catch (DaoException e) {
      throw new ServiceException("Cannot bind editions and authors", e);
    }
  }

  @Override
  public List<Author> findAuthors(Criteria criterion) throws ServiceException {
    try (LibraryDao<Author> authorDao =
        LibraryDaoFactory.getInstance().obtainDao(TableEntityMapper.AUTHOR)) {
      return authorDao.read(criterion, true);
    } catch (DaoException e) {
      throw new ServiceException("Cannot add a book", e);
    }
  }

  @Override
  public void decommissionBook(int bookId) throws ServiceException {
    try (LibraryDao<Book> bookDao =
        LibraryDaoFactory.getInstance().obtainDao(TableEntityMapper.BOOK)) {
      EntityField<Integer> field = new EntityField<>("locationId");
      field.setValue(ConstantManager.LOCATION_DECOMMISSIONED);
      bookDao.update(bookId, field);
    } catch (DaoException e) {
      throw new ServiceException("Cannot decommission a book", e);
    }
  }

  @Override
  public List<User> findUsers(String surname, String name) throws ServiceException {
    try (LibraryDao<User> userDao =
        LibraryDaoFactory.getInstance().obtainDao(TableEntityMapper.USER)) {
      EntityField<String> surnameField = new EntityField<>("surname");
      surnameField.setValue(surname);
      EntityField<String> nameField = new EntityField<>("name");
      nameField.setValue(name);
      Criteria criteria = new Criteria();
      criteria.addConstraints(surnameField, nameField);
      List<User> users = userDao.read(criteria, true);
      return users;
    } catch (DaoException e) {
      throw new ServiceException("Cannot find users", e);
    }
  }

  @Override
  public void addUser(User user) throws ServiceException {
    try (LibraryDao<User> userDao =
        LibraryDaoFactory.getInstance().obtainDao(TableEntityMapper.USER)) {
      userDao.add(user);
    } catch (DaoException e) {
      throw new ServiceException("Cannot add users", e);
    }
  }

  @Override
  public void changeStatus(int userId, int roleId) throws ServiceException {
    try (LibraryDao<User> userDao =
        LibraryDaoFactory.getInstance().obtainDao(TableEntityMapper.USER)) {
      EntityField<Integer> field = new EntityField<>("roleId");
      field.setValue(roleId);
      userDao.update(userId, field);
    } catch (DaoException e) {
      throw new ServiceException("Cannot change user status", e);
    }
  }

  @Override
  public void orderBook(BookOrder bookOrder) throws ServiceException {
    try (LibraryDao<Book> bookDao =
        LibraryDaoFactory.getInstance().obtainDao(TableEntityMapper.BOOK)) {
      int userId = bookOrder.getUserId();
      for (EditionInfo editionInfo : bookOrder.getEditionInfoSet()) {
        Book book = editionInfo.getBook();
        int bookId = book.getId();
        EntityField<Integer> userField = new EntityField<>("readerId");
        userField.setValue(userId);
        EntityField<Integer> reserveField = new EntityField<>("reserved");
        reserveField.setValue(ConstantManager.RESERVED);
        bookDao.update(bookId, userField);
        bookDao.update(bookId, reserveField);
      }
    } catch (DaoException e) {
      throw new ServiceException("Cannot order book", e);
    }
  }

  @Override
  public void prepareOrder(BookOrder bookOrder) throws ServiceException {
    try (LibraryDao<Book> bookDao =
        LibraryDaoFactory.getInstance().obtainDao(TableEntityMapper.BOOK)) {
      for (EditionInfo editionInfo : bookOrder.getEditionInfoSet()) {
        Book book = editionInfo.getBook();
        int bookId = book.getId();
        EntityField<Integer> locField = new EntityField<>("locationId");
        locField.setValue(ConstantManager.LOCATION_READING_HALL_RESERVE);
        EntityField<Integer> reserveField = new EntityField<>("reserved");
        reserveField.setValue(ConstantManager.PREPARED);
        bookDao.update(bookId, locField);
        bookDao.update(bookId, reserveField);
      }
    } catch (DaoException e) {
      throw new ServiceException("Cannot prepare book", e);
    }
  }

  @Override
  public void fulfillOrder(BookOrder dispatchedOrder) throws ServiceException {
    try (LibraryDao<Book> bookDao =
        LibraryDaoFactory.getInstance().obtainDao(TableEntityMapper.BOOK)) {
      for (EditionInfo editionInfo : dispatchedOrder.getEditionInfoSet()) {
        Book book = editionInfo.getBook();
        int bookId = book.getId();
        EntityField<Integer> locField = new EntityField<>("locationId");
        locField.setValue(ConstantManager.LOCATION_ON_HAND);
        EntityField<Integer> reserveField = new EntityField<>("reserved");
        reserveField.setValue(ConstantManager.NOT_RESERVED);
        bookDao.update(bookId, locField);
        bookDao.update(bookId, reserveField);
      }
    } catch (DaoException e) {
      throw new ServiceException("Cannot dispatch book", e);
    }
  }

  @Override
  public Book findBookById(int bookId) throws ServiceException {
    try (LibraryDao<Book> bookDao =
        LibraryDaoFactory.getInstance().obtainDao(TableEntityMapper.BOOK)) {
      return bookDao.get(bookId);
    } catch (DaoException e) {
      throw new ServiceException("Cannot find book", e);
    }
  }

  @Override
  public void fixReturn(Book book) throws ServiceException {
    try (LibraryDao<Book> bookDao =
        LibraryDaoFactory.getInstance().obtainDao(TableEntityMapper.BOOK)) {
      int bookId = book.getId();
      EntityField<Integer> userField = new EntityField<>("readerId");
      userField.setValue(null);
      EntityField<Integer> standardLocationField = book.fieldForName("standardLocationId");
      int locationId = standardLocationField.getValue();
      EntityField<Integer> locationField = new EntityField<>("locationId");
      locationField.setValue(locationId);
      bookDao.update(bookId, userField);
      bookDao.update(bookId, locationField);
    } catch (DaoException | LibraryEntityException e) {
      throw new ServiceException("Cannot return book", e);
    }
  }

  @Override
  public Collection<BookOrder> getPlacedOrders() throws ServiceException {
    List<BookOrder> placedOrders = new ArrayList<>();
    try (LibraryDao<Book> bookDao =
        LibraryDaoFactory.getInstance().obtainDao(TableEntityMapper.BOOK)) {
      EntityField<Integer> reserved = new EntityField<>("reserved");
      reserved.setValue(ConstantManager.RESERVED);
      Criteria criteria = new Criteria();
      criteria.addConstraint(reserved);
      List<Book> books = bookDao.read(criteria, true);
    } catch (DaoException e) {
      throw new ServiceException("Cannot get placed orders", e);
    }
    return null;
  }

  @Override
  public Collection<BookOrder> getPreparedOrders() {
    return null;
  }

  @Override
  public List<Book> findDeskBooksOnHands() {
    return null;
  }
}
