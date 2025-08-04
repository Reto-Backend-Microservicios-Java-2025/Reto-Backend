package pe.upc.edu.customerservice.interfaces.rest.resources;

import pe.upc.edu.customerservice.domain.model.valueobjects.TypeDocument;

public record ClientResource(
        String full_name,
        String full_last_name,
        TypeDocument type_document,
        String number_document,
        String uniqueCode
) {
}

