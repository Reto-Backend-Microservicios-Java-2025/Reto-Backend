/**
 * UpdateProductCommand
 * @Summary
 *  UpdateProductCommand is a record class that represents the command to update a product
 **/

package pe.upc.edu.productservice.domain.model.commands;

import pe.upc.edu.productservice.domain.model.valueobjects.ProductType;

public record UpdateProductCommand(
        Long id,
        ProductType productType,
        String name,
        Double balance
) {
}