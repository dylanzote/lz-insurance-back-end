package com.lz_Insurance.product.engine.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lz_Insurance.core.exception.ErrorCode;
import com.lz_Insurance.product.engine.exception.SchemaValidationException;
import com.lz_Insurance.product.engine.model.ProductSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JsonSchemaParser {

    private final ObjectMapper objectMapper;

    public ProductSchema parse(String jsonSchema) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonSchema);

            if (!rootNode.has("productId")) {
                throw new SchemaValidationException(ErrorCode.VALIDATION_FAILED, "Product schema must contain productId");
            }
            if (!rootNode.has("productName")) {
                throw new SchemaValidationException(ErrorCode.VALIDATION_FAILED, "Product schema must contain productName");
            }
            if (!rootNode.has("coverages")) {
                throw new SchemaValidationException(ErrorCode.VALIDATION_FAILED, "Product schema must contain coverages");
            }

            return objectMapper.treeToValue(rootNode, ProductSchema.class);
        } catch (SchemaValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error parsing product schema", e);
            throw new SchemaValidationException(ErrorCode.INTERNAL_SERVER_ERROR, e);
        }
    }

    public ProductSchema parse(JsonNode jsonNode) {
        try {
            return objectMapper.treeToValue(jsonNode, ProductSchema.class);
        } catch (Exception e) {
            log.error("Error parsing product schema from JsonNode", e);
            throw new SchemaValidationException(ErrorCode.INTERNAL_SERVER_ERROR, e);
        }
    }
}
