package com.example.multirealm.repository;

import com.example.multirealm.entity.Tenant;
import com.example.multirealm.entity.TenantStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Tenant records in the application database. */
@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {

    Optional<Tenant> findByCode(String code);

    boolean existsByCode(String code);

    List<Tenant> findByStatus(TenantStatus status);
}
