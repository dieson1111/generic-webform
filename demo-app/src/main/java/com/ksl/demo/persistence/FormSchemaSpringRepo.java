package com.ksl.demo.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for {@link FormSchemaEntity}.
 */
public interface FormSchemaSpringRepo extends JpaRepository<FormSchemaEntity, String> {
}
