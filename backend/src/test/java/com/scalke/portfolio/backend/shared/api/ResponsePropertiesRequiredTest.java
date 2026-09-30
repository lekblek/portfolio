package com.scalke.portfolio.backend.shared.api;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResponsePropertiesRequiredTest {

    private static Schema<?> ref(String name) {
        return new Schema<>().$ref("#/components/schemas/" + name);
    }

    private static Schema<?> object(String... properties) {
        Schema<?> schema = new Schema<>();
        for (String property : properties) {
            schema.addProperty(property, new Schema<>());
        }
        return schema;
    }

    private static Content json(Schema<?> schema) {
        return new Content().addMediaType("application/json", new MediaType().schema(schema));
    }

    private static OpenAPI contract() {
        Schema<?> profile = object("name", "avatar", "links")
            .addProperty("avatar", new Schema<>().oneOf(List.of(ref("Image"), new Schema<>())))
            .addProperty("links", new ArraySchema().items(ref("Link")));
        Operation read = new Operation().responses(new ApiResponses()
            .addApiResponse("200", new ApiResponse().content(json(ref("Profile")))));
        Operation save = new Operation()
            .requestBody(new RequestBody().content(json(ref("SaveProfile"))))
            .responses(new ApiResponses().addApiResponse("200", new ApiResponse().content(json(ref("Profile")))));
        return new OpenAPI()
            .paths(new Paths()
                .addPathItem("/profile", new PathItem().get(read))
                .addPathItem("/admin/profile", new PathItem().put(save)))
            .components(new Components()
                .addSchemas("Profile", profile)
                .addSchemas("Image", object("width", "url"))
                .addSchemas("Link", object("url", "label"))
                .addSchemas("SaveProfile", object("name", "links")
                    .addProperty("links", new ArraySchema().items(ref("Link")))
                    .required(List.of("name"))));
    }

    @Test
    void every_property_of_a_response_schema_is_required_through_references_arrays_and_compositions() {
        OpenAPI contract = contract();

        new ResponsePropertiesRequired().customise(contract);

        assertThat(contract.getComponents().getSchemas().get("Profile").getRequired())
            .containsExactly("avatar", "links", "name");
        assertThat(contract.getComponents().getSchemas().get("Image").getRequired())
            .containsExactly("url", "width");
    }

    @Test
    void a_schema_also_sent_in_a_request_keeps_the_required_properties_of_its_validation() {
        OpenAPI contract = contract();

        new ResponsePropertiesRequired().customise(contract);

        assertThat(contract.getComponents().getSchemas().get("SaveProfile").getRequired()).containsExactly("name");
        assertThat(contract.getComponents().getSchemas().get("Link").getRequired()).isNull();
    }
}
