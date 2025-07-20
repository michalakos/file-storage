package com.mvasilakos.filestorage.mapper;

import java.util.List;
import java.util.stream.Collectors;


/**
 * Entity mapper interface.
 *
 * @param <E> entity class
 * @param <D> dto class
 */
public interface EntityMapper<E, D> {

  /**
   * Convert an entity to dto.
   *
   * @param entity entity instance
   * @return dto instance
   */
  D toDto(E entity);

  /**
   * Convert a dto to entity.
   *
   * @param dto dto instance
   * @return entity instance
   */
  E toEntity(D dto);

  /**
   * Convert a list of entities to a list of dtos.
   *
   * @param entities entities
   * @return dtos
   */
  default List<D> toDtoList(List<E> entities) {
    if (entities == null) {
      return null;
    }
    return entities.stream()
        .map(this::toDto)
        .collect(Collectors.toList());
  }

  /**
   * Convert a list of dtos to a list of entities.
   *
   * @param dtos dtos
   * @return entities
   */
  default List<E> toEntityList(List<D> dtos) {
    if (dtos == null) {
      return null;
    }
    return dtos.stream()
        .map(this::toEntity)
        .collect(Collectors.toList());
  }

}
