package com.monosun.secportal.asset.repository;

import com.monosun.secportal.asset.entity.Asset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AssetRepository extends JpaRepository<Asset, Long> {

    @Query("SELECT a FROM Asset a WHERE " +
           "(:keyword IS NULL OR LOWER(a.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           " OR LOWER(a.ipAddress) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           " OR LOWER(a.owner) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           " OR LOWER(a.cloudResourceId) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           " OR LOWER(a.region) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:type IS NULL OR a.type = :type) AND " +
           "(:criticality IS NULL OR a.criticality = :criticality) AND " +
           "(:cloudProvider IS NULL OR a.cloudProvider = :cloudProvider) AND " +
           "(:environment IS NULL OR a.environment = :environment) AND " +
           "(:active IS NULL OR a.active = :active) AND " +
           "(:status IS NULL OR a.status = :status)")
    Page<Asset> search(
            @Param("keyword") String keyword,
            @Param("type") String type,
            @Param("criticality") Asset.Criticality criticality,
            @Param("cloudProvider") Asset.CloudProvider cloudProvider,
            @Param("environment") Asset.Environment environment,
            @Param("active") Boolean active,
            @Param("status") Asset.Status status,
            Pageable pageable);

    long countByActive(boolean active);
    long countByCriticality(Asset.Criticality criticality);

    /** 자산유형별 자산 수 */
    @Query("SELECT a.type AS type, COUNT(a) AS total FROM Asset a GROUP BY a.type ORDER BY a.type")
    List<TypeCount> countByType();

    @Modifying
    @Query("DELETE FROM Asset a WHERE a.type = :type")
    int deleteByType(@Param("type") String type);

    @Modifying
    @Query("UPDATE Asset a SET a.sbomSoftware = null WHERE a.sbomSoftware.id = :sbomSoftwareId")
    void clearSbomSoftware(@Param("sbomSoftwareId") Long sbomSoftwareId);

    /** 자산유형별 자산 수 프로젝션 */
    interface TypeCount {
        String getType();
        long getTotal();
    }
}
