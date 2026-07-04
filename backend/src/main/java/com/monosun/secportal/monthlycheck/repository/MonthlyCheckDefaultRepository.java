package com.monosun.secportal.monthlycheck.repository;

import com.monosun.secportal.monthlycheck.entity.MonthlyCheckDefault;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MonthlyCheckDefaultRepository extends JpaRepository<MonthlyCheckDefault, Long> {

    List<MonthlyCheckDefault> findAllByOrderBySortOrderAsc();

    List<MonthlyCheckDefault> findByActiveOrderBySortOrderAsc(boolean active);
}
