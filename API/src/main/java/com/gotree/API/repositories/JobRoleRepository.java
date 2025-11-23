package com.gotree.API.repositories;

import com.gotree.API.entities.Company;
import com.gotree.API.entities.JobRole;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JobRoleRepository extends JpaRepository<JobRole, Long> {
    List<JobRole> findByCompanyOrderByNameAsc(Company company);
    boolean existsByNameAndCompany(String name, Company company);

    void deleteByCompanyId(Long companyId);
}