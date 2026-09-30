package com.scalke.portfolio.backend.shared.api;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.Schema;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.lang.reflect.RecordComponent;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * KI-34 : une composante de record annotée {@link Nullable} (JSpecify) peut valoir {@code null} dans le JSON ; le
 * contrat OpenAPI 3.1 l'écrit ({@code "type": ["string", "null"]}, ou {@code oneOf} avec {@code null} pour un objet
 * référencé). Une composante sans annotation ne vaut jamais {@code null}. Ce que le code déclare, le contrat le dit :
 * le schéma n'est jamais corrigé à la main (D-DZ).
 */
@Component
class NullableRecordComponents implements ModelConverter {

    private static final String REF_PREFIX = "#/components/schemas/";
    private static final String NULL_TYPE = "null";

    @Override
    public @Nullable Schema<?> resolve(AnnotatedType type, ModelConverterContext context,
                                       Iterator<ModelConverter> chain) {
        Schema<?> resolved = chain.hasNext() ? chain.next().resolve(type, context, chain) : null;
        if (resolved == null) {
            return null;
        }
        Class<?> rawClass = Json.mapper().constructType(type.getType()).getRawClass();
        if (rawClass.isRecord()) {
            Schema<?> model = definition(resolved, context);
            if (model != null && model.getProperties() != null) {
                markNullableComponents(rawClass, model.getProperties());
            }
        }
        return resolved;
    }

    private static @Nullable Schema<?> definition(Schema<?> resolved, ModelConverterContext context) {
        String ref = resolved.get$ref();
        return ref == null ? resolved : context.getDefinedModels().get(ref.substring(REF_PREFIX.length()));
    }

    @SuppressWarnings("rawtypes")
    private static void markNullableComponents(Class<?> record, Map<String, Schema> properties) {
        for (RecordComponent component : record.getRecordComponents()) {
            Schema<?> property = properties.get(component.getName());
            if (property != null && component.getAnnotatedType().isAnnotationPresent(Nullable.class)) {
                properties.put(component.getName(), nullable(property));
            }
        }
    }

    /**
     * Idempotent : un type déjà résolu peut repasser par le convertisseur.
     */
    private static Schema<?> nullable(Schema<?> property) {
        if (property.get$ref() != null) {
            return new Schema<>().oneOf(List.of(new Schema<>().$ref(property.get$ref()), nullSchema()));
        }
        if (property.getTypes() != null && !property.getTypes().contains(NULL_TYPE)) {
            property.addType(NULL_TYPE);
        }
        return property;
    }

    private static Schema<?> nullSchema() {
        Schema<?> schema = new Schema<>();
        schema.addType(NULL_TYPE);
        return schema;
    }
}
