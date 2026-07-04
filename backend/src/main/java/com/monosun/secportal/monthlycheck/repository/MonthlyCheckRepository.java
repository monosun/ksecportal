package com.monosun.secportal.monthlycheck.repository;

import com.monosun.secportal.monthlycheck.entity.MonthlyCheckItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MonthlyCheckRepository extends JpaRepository<MonthlyCheckItem, Long> {

    List<MonthlyCheckItem> findByYearMonthOrderBySortOrderAscIdAsc(String yearMonth);

    @Query("SELECT DISTINCT m.yearMonth FROM MonthlyCheckItem m ORDER BY m.yearMonth DESC")
    List<String> findDistinctYearMonths();
}
