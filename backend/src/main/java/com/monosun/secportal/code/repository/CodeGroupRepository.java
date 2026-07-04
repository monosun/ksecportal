package com.monosun.secportal.code.repository;

import com.monosun.secportal.code.entity.CodeGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CodeGroupRepository extends JpaRepository<CodeGroup, String> {
    List<CodeGroup> findAllByOrderBySortOrderAscGroupNameAsc();
}
