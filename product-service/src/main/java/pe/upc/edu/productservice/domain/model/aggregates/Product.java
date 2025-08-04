/**
 * Product aggregates
 * @Summary
 * The product class is an aggregates root that represents a product in the system.
 */
package pe.upc.edu.productservice.domain.model.aggregates;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import pe.upc.edu.productservice.domain.model.commands.CreateProductCommand;
import pe.upc.edu.productservice.domain.model.valueobjects.ProductType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table("products")
public class Product {
    @Id
    private Long id;

    @Column("client_id")
    private Long clientId;

    @Column("product_type")
    private ProductType productType;

    @Column("name")
    private String name;

    @Column("balance")
    private Double balance;

    public Product(Long clientId, ProductType productType, String name, Double balance) {
        this.clientId = clientId;
        this.productType = productType;
        this.name = name;
        this.balance = balance;
    }

    public Product(CreateProductCommand command) {
        this.clientId = command.clientId();
        this.productType = command.productType();
        this.name = command.name();
        this.balance = command.balance();
    }

    public Product updateInformation(ProductType productType, String name, Double balance) {
        this.productType = productType;
        this.name = name;
        this.balance = balance;
        return this;
    }
}