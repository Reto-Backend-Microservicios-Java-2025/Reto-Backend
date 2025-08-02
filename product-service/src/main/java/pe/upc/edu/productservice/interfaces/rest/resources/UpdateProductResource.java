/**
 * UpdateProductResource
 * @Summary
 *  UpdateProductResource is a record class that represents the resource to update a product
 **/

package pe.upc.edu.productservice.interfaces.rest.resources;

import pe.upc.edu.productservice.domain.model.valueobjects.ProductType;

public record UpdateProductResource(ProductType productType, String name, Double balance) {
}