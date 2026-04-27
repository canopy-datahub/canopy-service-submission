package org.canopyplatform.canopy.submissionservice.repositories;

import org.canopyplatform.canopy.submissionservice.models.LkupPropertyCodelistValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LkupPropertyCodelistValueRepository extends JpaRepository<LkupPropertyCodelistValue, Integer> {

    List<LkupPropertyCodelistValue> findAllByPropertyCodelistId(Integer codelistId);

}
