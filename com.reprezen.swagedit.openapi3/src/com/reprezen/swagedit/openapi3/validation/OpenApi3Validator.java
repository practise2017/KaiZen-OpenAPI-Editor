package com.reprezen.swagedit.openapi3.validation;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.core.resources.IMarker;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.reprezen.swagedit.core.json.references.JsonReferenceValidator;
import com.reprezen.swagedit.core.model.AbstractNode;
import com.reprezen.swagedit.core.model.Model;
import com.reprezen.swagedit.core.validation.Messages;
import com.reprezen.swagedit.core.validation.SwaggerError;
import com.reprezen.swagedit.core.validation.Validator;

public class OpenApi3Validator extends Validator {

    private final JsonPointer operationPointer = JsonPointer.compile("/definitions/operation");
    private final JsonPointer securityPointer = JsonPointer.compile("/components/securitySchemes");

    public OpenApi3Validator(JsonReferenceValidator referenceValidator, Map<String, JsonNode> preloadedSchemas) {
        super(referenceValidator, preloadedSchemas);
    }

    @Override
    protected void executeModelValidation(Model model, AbstractNode node, Set<SwaggerError> errors) {
        super.executeModelValidation(model, node, errors);
        validateOperationIdReferences(model, node, errors);
        validateOperationRefReferences(model, node, errors);
        validateSecuritySchemeReferences(model, node, errors);
    }

    private void validateSecuritySchemeReferences(Model model, AbstractNode node, Set<SwaggerError> errors) {
        if (node.getPointerString().matches(".*/security/\\d+")) {
            AbstractNode securitySchemes = model.find(securityPointer);

            if (node.isObject()) {
                for (String field : node.asObject().fieldNames()) {
                    if (securitySchemes.get(field) == null) {
                        errors.add(
                                error(node.get(field), IMarker.SEVERITY_ERROR, Messages.error_invalid_reference_type));
                    }
                }
            }
        }
    }

    private void validateOperationRefReferences(Model model, AbstractNode node, Set<SwaggerError> errors) {
        JsonPointer schemaPointer = JsonPointer.compile("/definitions/link/properties/operationRef");

        if (node != null && node.getType() != null && schemaPointer.equals(node.getType().getPointer())) {            
            String operationRefPointer = (String) node.asValue().getValue();
            AbstractNode operation = model.find(operationRefPointer);

            if (operation == null) {
                errors.add(error(node, IMarker.SEVERITY_ERROR, Messages.error_invalid_reference));
            } else if (operation.getType() == null
                    || !Objects.equals(operationPointer, operation.getType().getPointer())) {
                errors.add(error(node, IMarker.SEVERITY_ERROR, Messages.error_invalid_reference_type));
            }
        }
    }

    protected void validateOperationIdReferences(Model model, AbstractNode node, Set<SwaggerError> errors) {
        JsonPointer schemaPointer = JsonPointer.compile("/definitions/link/properties/operationId");

        if (node != null && node.getType() != null && schemaPointer.equals(node.getType().getPointer())) {
            List<AbstractNode> nodes = model.findByType(operationPointer);
            Iterator<AbstractNode> it = nodes.iterator();

            boolean found = false;
            while (it.hasNext() && !found) {
                AbstractNode current = it.next();
                AbstractNode value = current.get("operationId");

                found = value != null && Objects.equals(node.asValue().getValue(), value.asValue().getValue());
            }

            if (!found) {
                errors.add(error(node, IMarker.SEVERITY_ERROR, Messages.error_invalid_reference_type));
            }
        }
    }

}
