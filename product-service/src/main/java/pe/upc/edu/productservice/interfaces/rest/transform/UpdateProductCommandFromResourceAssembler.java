package pe.upc.edu.productservice.interfaces.rest.transform;

import pe.upc.edu.productservice.domain.model.commands.UpdateProductCommand;
import pe.upc.edu.productservice.interfaces.rest.resources.UpdateProductResource;

public class UpdateProductCommandFromResourceAssembler {
    public static UpdateProductCommand toCommandFromResource(Long productId, UpdateProductResource resource) {
        return new UpdateProductCommand(
                productId,
                resource.productType(),
                resource.name(),
                resource.balance());
    }
}