package com.neml.badminton.repository;

import com.neml.badminton.entity.AccountActivation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface AccountActivationRepository extends JpaRepository<AccountActivation,UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AccountActivation a where a.tokenHash = :hash")
    Optional<AccountActivation> findForUpdate(@Param("hash") String hash);
}
