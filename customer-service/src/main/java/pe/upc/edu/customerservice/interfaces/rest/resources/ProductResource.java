package pe.upc.edu.customerservice.interfaces.rest.resources;

public record ProductResource(
        Long id,
        String productType,
        String name,
        Double balance
) {}
