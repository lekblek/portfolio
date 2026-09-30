package com.scalke.portfolio.backend.shared.api;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import org.jspecify.annotations.Nullable;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * KI-34 : Jackson écrit toutes les composantes d'un record, {@code null} compris (aucune inclusion restreinte) ; toute
 * propriété d'un schéma de réponse est donc présente, donc {@code required}. Qu'elle puisse valoir {@code null} est
 * dit à part ({@link NullableRecordComponents}).
 * <p>
 * Un schéma atteint aussi depuis un corps de requête garde la sémantique de la requête (propriétés obligatoires
 * déduites de la validation) : une propriété facultative peut y être omise (D-DZ).
 */
@Component
class ResponsePropertiesRequired implements OpenApiCustomizer {

    private static final String REF_PREFIX = "#/components/schemas/";

    @Override
    @SuppressWarnings("rawtypes")
    public void customise(OpenAPI openApi) {
        if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
            return;
        }
        Map<String, Schema> schemas = openApi.getComponents().getSchemas();
        List<Operation> operations = openApi.getPaths() == null ? List.of() : openApi.getPaths().values().stream()
            .flatMap(path -> path.readOperations().stream())
            .toList();

        Set<String> requestSchemas = reachable(schemas, operations.stream()
            .filter(operation -> operation.getRequestBody() != null)
            .flatMap(operation -> schemasOf(operation.getRequestBody().getContent())));
        Set<String> responseSchemas = reachable(schemas, operations.stream()
            .filter(operation -> operation.getResponses() != null)
            .flatMap(operation -> operation.getResponses().values().stream())
            .flatMap(response -> schemasOf(response.getContent())));

        responseSchemas.removeAll(requestSchemas);
        responseSchemas.stream()
            .map(schemas::get)
            .filter(schema -> schema != null && schema.getProperties() != null && !schema.getProperties().isEmpty())
            .forEach(schema -> schema.setRequired(new ArrayList<>(new TreeSet<String>(schema.getProperties().keySet()))));
    }

    private static Stream<Schema<?>> schemasOf(@Nullable Content content) {
        if (content == null) {
            return Stream.empty();
        }
        return content.values().stream()
            .<Schema<?>>map(mediaType -> mediaType.getSchema())
            .filter(Objects::nonNull);
    }

    /**
     * Noms des schémas nommés atteints depuis les racines, en suivant propriétés, éléments et compositions.
     */
    @SuppressWarnings("rawtypes")
    private static Set<String> reachable(Map<String, Schema> schemas, Stream<Schema<?>> roots) {
        Set<String> names = new HashSet<>();
        Deque<Schema<?>> pending = new ArrayDeque<>(roots.toList());
        while (!pending.isEmpty()) {
            Schema<?> schema = pending.pop();
            String ref = schema.get$ref();
            if (ref != null) {
                String name = ref.substring(REF_PREFIX.length());
                if (names.add(name) && schemas.get(name) != null) {
                    pending.push(schemas.get(name));
                }
                continue;
            }
            children(schema).forEach(pending::push);
        }
        return names;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static List<Schema<?>> children(Schema<?> schema) {
        List<Schema<?>> children = new ArrayList<>();
        if (schema.getProperties() != null) {
            children.addAll((Collection) schema.getProperties().values());
        }
        if (schema.getItems() != null) {
            children.add(schema.getItems());
        }
        Stream.of(schema.getOneOf(), schema.getAnyOf(), schema.getAllOf())
            .filter(Objects::nonNull)
            .forEach(composed -> children.addAll((Collection) composed));
        if (schema.getAdditionalProperties() instanceof Schema<?> additional) {
            children.add(additional);
        }
        return children;
    }
}
