package com.gotree.API.repositories;

import com.gotree.API.entities.ReportItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportItemRepository extends JpaRepository<ReportItem, Long> {
}