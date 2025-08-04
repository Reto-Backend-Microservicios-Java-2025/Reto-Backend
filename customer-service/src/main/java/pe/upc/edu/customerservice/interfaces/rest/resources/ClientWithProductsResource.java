package pe.upc.edu.customerservice.interfaces.rest.resources;

import java.util.List;

public record ClientWithProductsResource(
        Long id,
        String full_name,
        String full_lastName,
        String type_document,
        String number_document,
        Long uniqueCode,
        List<ProductResource> products
) {}
