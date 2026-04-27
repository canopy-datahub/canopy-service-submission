package org.canopyplatform.canopy.submissionservice.repositories;

import org.canopyplatform.canopy.submissionservice.models.LkupCenter;
import org.canopyplatform.canopy.submissionservice.models.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsersRepository extends JpaRepository<Users, Integer> {

	List<Users> findAllByRoles_NameAndInstitutionId(String role, Integer institutionId);

	List<Users> findAllByRoles_Name(String role);

	@Query(
			value = "select distinct u.* from users u " +
					"join data_submission ds on ds.submitter_user_id = u.id " +
					"where ds.id=:submissionId",
			nativeQuery = true
	)
	Optional<Users> findBySubmissionId(Integer submissionId);

	Optional<Users> findUsersBySftpPathEqualsIgnoreCase(String sftpPath);

	List<Users> findAllByRoles_NameAndCenter(String role, LkupCenter center);

	List<Users> findAllByRoles_NameAndCenterAndInternalUserIsTrue(String role, LkupCenter center);

	List<Users> findAllByRoles_NameAndCenterAndInternalUserIsFalse(String role, LkupCenter center);
}
