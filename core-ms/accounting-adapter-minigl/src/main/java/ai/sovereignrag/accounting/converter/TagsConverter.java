package ai.sovereignrag.accounting.converter;

import ai.sovereignrag.accounting.Tags;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA AttributeConverter for converting between Tags object and String representation.
 * This replaces the legacy Hibernate UserType TagsType.
 */
@Converter
public class TagsConverter implements AttributeConverter<Tags, String> {

    @Override
    public String convertToDatabaseColumn(Tags tags) {
        if (tags == null) {
            return null;
        }
        return tags.toString();
    }

    @Override
    public Tags convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        return new Tags(dbData);
    }
}