package TicketCodeIA.entity;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StringListConverterTest {

    private final StringListConverter converter = new StringListConverter();

    @Test
    void convertToDatabaseColumn_withElements_returnsJsonArray() {
        String result = converter.convertToDatabaseColumn(List.of("log1", "log2", "log3"));
        assertThat(result).isEqualTo("[\"log1\",\"log2\",\"log3\"]");
    }

    @Test
    void convertToDatabaseColumn_withNull_returnsEmptyJsonArray() {
        String result = converter.convertToDatabaseColumn(null);
        assertThat(result).isEqualTo("[]");
    }

    @Test
    void convertToDatabaseColumn_withEmptyList_returnsEmptyJsonArray() {
        String result = converter.convertToDatabaseColumn(List.of());
        assertThat(result).isEqualTo("[]");
    }

    @Test
    void convertToDatabaseColumn_withSingleElement() {
        String result = converter.convertToDatabaseColumn(List.of("only one"));
        assertThat(result).isEqualTo("[\"only one\"]");
    }

    @Test
    void convertToEntityAttribute_withJsonArray_returnsList() {
        List<String> result = converter.convertToEntityAttribute("[\"a\",\"b\",\"c\"]");
        assertThat(result).containsExactly("a", "b", "c");
    }

    @Test
    void convertToEntityAttribute_withNull_returnsEmptyList() {
        List<String> result = converter.convertToEntityAttribute(null);
        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    void convertToEntityAttribute_withEmptyString_returnsEmptyList() {
        List<String> result = converter.convertToEntityAttribute("");
        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    void convertToEntityAttribute_withEmptyJsonArray_returnsEmptyList() {
        List<String> result = converter.convertToEntityAttribute("[]");
        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    void convertToEntityAttribute_withInvalidJson_returnsEmptyList() {
        List<String> result = converter.convertToEntityAttribute("not-valid-json");
        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    void roundTrip_preservesData() {
        List<String> original = List.of("agent started", "processing ticket", "done");
        String serialized = converter.convertToDatabaseColumn(original);
        List<String> deserialized = converter.convertToEntityAttribute(serialized);
        assertThat(deserialized).isEqualTo(original);
    }
}
