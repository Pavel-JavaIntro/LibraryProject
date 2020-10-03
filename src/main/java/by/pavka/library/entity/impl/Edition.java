package by.pavka.library.entity.impl;

import by.pavka.library.entity.LibraryEntity;
import by.pavka.library.entity.criteria.EntityField;

public class Edition extends LibraryEntity {

  @Override
  public EntityField[] listFields() {
    EntityField<String> standardNumber = new EntityField<>("standard_number");
    EntityField<String> title = new EntityField<>("title");
    EntityField<Integer> year = new EntityField<>("year");
    EntityField<Integer> genreId = new EntityField<>("genre_id");
    EntityField<Integer> deliveries = new EntityField<>("deliveries");
    return new EntityField[] {standardNumber, title, year, genreId, deliveries};
  }
}