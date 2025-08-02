/**
 * CreateProductCommand
 * @Summary
 *  CreateProductCommand is a record class that represents the command create
 **/

package pe.upc.edu.productservice.domain.model.commands;

import pe.upc.edu.productservice.domain.model.valueobjects.ProductType;

public record CreateProductCommand(
        ProductType productType,
        String name,
        Double balance
) {
}