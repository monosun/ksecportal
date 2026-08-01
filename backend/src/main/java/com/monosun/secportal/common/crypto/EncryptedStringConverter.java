package com.monosun.secportal.common.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * 개인정보 컬럼을 저장할 때 암호화하고 읽을 때 복호화하는 JPA 컨버터.
 *
 * <p>검색·정렬·유니크 제약에 쓰이지 않는 컬럼에만 적용한다. 암호문은 매번 IV가 달라
 * 같은 값이라도 다른 문자열이 되므로 {@code WHERE col = ?} 로는 찾을 수 없다.</p>
 *
 * <p>적용한 컬럼은 길이를 넉넉히 늘려야 한다({@code db/migration} 참고).</p>
 */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return ColumnCipher.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return ColumnCipher.decrypt(dbData);
    }
}
