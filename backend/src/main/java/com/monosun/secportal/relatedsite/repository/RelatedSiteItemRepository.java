package com.monosun.secportal.relatedsite.repository;

import com.monosun.secportal.relatedsite.entity.RelatedSiteItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RelatedSiteItemRepository extends JpaRepository<RelatedSiteItem, Long> {

    List<RelatedSiteItem> findBySiteIdOrderBySortOrderAscIdAsc(Long siteId);

    List<RelatedSiteItem> findBySiteIdInOrderBySortOrderAscIdAsc(List<Long> siteIds);

    void deleteBySiteId(Long siteId);
}
