-- v1.25.0 — 위협 카탈로그 대상 자산유형(복수) 컬럼 추가 + 기존 위협 전건 자산유형 할당
-- asset_types 컬럼 자체는 ddl-auto 로도 추가되지만, 구버전 볼륨에서 수동 실행할 수 있도록 함께 둔다.
-- 값은 코드관리 ASSET_TYPE 그룹의 코드값이며, 자산분류(asset_detail) 기준으로 일괄 할당한다.
-- 실행: docker compose exec -T db mysql -usecportal -psecportal123 secportal < db/migration/v1.25.0_threat_asset_types.sql

SET NAMES utf8mb4;

-- 컬럼 추가 (이미 있으면 오류가 나므로 필요 시에만 주석 해제)
-- ALTER TABLE threats ADD COLUMN asset_types VARCHAR(500) NULL AFTER asset_detail;
-- ALTER TABLE threat_defaults ADD COLUMN asset_types VARCHAR(500) NULL AFTER asset_detail;

-- 자산분류 → 대상 자산유형 매핑 (이미 지정된 항목은 건드리지 않는다)
UPDATE threats SET asset_types = CASE asset_detail
    WHEN 'SSO/MFA'     THEN 'APPLICATION,SERVER'
    WHEN 'IAM/S3'      THEN 'S3,CLOUD_OTHER'
    WHEN 'Kubernetes'  THEN 'EC2,CLOUD_OTHER,APPLICATION'
    WHEN 'DB'          THEN 'DATABASE,RDS'
    WHEN 'IDC'         THEN 'SERVER,NETWORK,WORKSTATION'
    WHEN 'RAG'         THEN 'APPLICATION,DATABASE,CLOUD_OTHER'
    WHEN '생성형AI'    THEN 'APPLICATION,CLOUD_OTHER'
    WHEN '결제/정산'   THEN 'APPLICATION,DATABASE'
    WHEN '고객포털'    THEN 'APPLICATION,ELB,EC2'
    WHEN '배포플랫폼'  THEN 'APPLICATION,CLOUD_OTHER'
    WHEN '셀프개통'    THEN 'APPLICATION,DATABASE'
    WHEN '소스코드'    THEN 'APPLICATION,CLOUD_OTHER'
    WHEN '협력사'      THEN 'APPLICATION,OTHER'
    WHEN '회원정보'    THEN 'DATABASE,APPLICATION'
    ELSE 'APPLICATION,SERVER'
END
WHERE asset_types IS NULL OR asset_types = '';

UPDATE threat_defaults SET asset_types = CASE asset_detail
    WHEN 'SSO/MFA'     THEN 'APPLICATION,SERVER'
    WHEN 'IAM/S3'      THEN 'S3,CLOUD_OTHER'
    WHEN 'Kubernetes'  THEN 'EC2,CLOUD_OTHER,APPLICATION'
    WHEN 'DB'          THEN 'DATABASE,RDS'
    WHEN 'IDC'         THEN 'SERVER,NETWORK,WORKSTATION'
    WHEN 'RAG'         THEN 'APPLICATION,DATABASE,CLOUD_OTHER'
    WHEN '생성형AI'    THEN 'APPLICATION,CLOUD_OTHER'
    WHEN '결제/정산'   THEN 'APPLICATION,DATABASE'
    WHEN '고객포털'    THEN 'APPLICATION,ELB,EC2'
    WHEN '배포플랫폼'  THEN 'APPLICATION,CLOUD_OTHER'
    WHEN '셀프개통'    THEN 'APPLICATION,DATABASE'
    WHEN '소스코드'    THEN 'APPLICATION,CLOUD_OTHER'
    WHEN '협력사'      THEN 'APPLICATION,OTHER'
    WHEN '회원정보'    THEN 'DATABASE,APPLICATION'
    ELSE 'APPLICATION,SERVER'
END
WHERE asset_types IS NULL OR asset_types = '';
