package pe.upc.edu.productservice.interfaces.rest.transform;

import pe.upc.edu.productservice.domain.model.aggregates.Product;
import pe.upc.edu.productservice.interfaces.rest.resources.ProductResource;

public class ProductResourceFromEntityAssembler {
    public static ProductResource toResourceFromEntity(Product entity) {
        return new ProductResource(
                entity.getId(),
                entity.getProductType(),
                entity.getName(),
                entity.getBalance());
    }
}