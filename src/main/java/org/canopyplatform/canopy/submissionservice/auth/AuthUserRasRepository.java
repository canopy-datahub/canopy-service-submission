package org.canopyplatform.canopy.submissionservice.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthUserRasRepository extends JpaRepository<AuthUserRas, Integer> {

    List<AuthUserRas> findAllByUserId(Integer userId);

}
