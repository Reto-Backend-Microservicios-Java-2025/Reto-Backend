/**
 * ProductResource
 * @Summary
 *  ProductResource is a record class that represents the resource response for a product
 **/

package pe.upc.edu.productservice.interfaces.rest.resources;

import pe.upc.edu.productservice.domain.model.valueobjects.ProductType;

public record ProductResource(Long id, ProductType productType, String name, Double balance) {
}