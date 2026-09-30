package com.scalke.portfolio.backend.shared.api;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.media.Schema;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NullableRecordComponentsTest {

    record Picture(String url, @Nullable String altText) {
    }

    record Period(String label, LocalDate startDate, @Nullable LocalDate endDate, @Nullable Picture picture,
                  Picture banner, List<String> tags) {
    }

    @SuppressWarnings("rawtypes")
    private static Map<String, Schema> schemas() {
        ModelConverters converters = new ModelConverters(true);
        converters.addConverter(new NullableRecordComponents());
        return converters.readAll(Period.class);
    }

    @SuppressWarnings("rawtypes")
    private static Schema property(String schema, String property) {
        return (Schema) schemas().get(schema).getProperties().get(property);
    }

    @Test
    void a_nullable_scalar_accepts_null_in_addition_to_its_type() {
        assertThat(property("Period", "endDate").getTypes()).containsExactlyInAnyOrder("string", "null");
        assertThat(property("Period", "endDate").getFormat()).isEqualTo("date");
        assertThat(property("Picture", "altText").getTypes()).containsExactlyInAnyOrder("string", "null");
    }

    @Test
    void a_nullable_object_is_a_reference_or_null() {
        Schema<?> picture = property("Period", "picture");

        assertThat(picture.get$ref()).isNull();
        assertThat(picture.getOneOf()).hasSize(2);
        assertThat(picture.getOneOf().get(0).get$ref()).isEqualTo("#/components/schemas/Picture");
        assertThat(picture.getOneOf().get(1).getTypes()).containsExactly("null");
    }

    @Test
    void a_component_without_the_annotation_is_never_null() {
        assertThat(property("Period", "label").getTypes()).containsExactly("string");
        assertThat(property("Period", "startDate").getTypes()).containsExactly("string");
        assertThat(property("Period", "banner").get$ref()).isEqualTo("#/components/schemas/Picture");
        assertThat(property("Period", "tags").getTypes()).containsExactly("array");
        assertThat(property("Picture", "url").getTypes()).containsExactly("string");
    }
}
