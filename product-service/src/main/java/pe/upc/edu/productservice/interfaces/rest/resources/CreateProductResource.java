/**
 * CreateProductResource
 * @Summary
 *  CreateProductResource is a record class that represents the resource to create a product
 **/

package pe.upc.edu.productservice.interfaces.rest.resources;

import pe.upc.edu.productservice.domain.model.valueobjects.ProductType;

public record CreateProductResource(ProductType productType, String name, Double balance) {
}