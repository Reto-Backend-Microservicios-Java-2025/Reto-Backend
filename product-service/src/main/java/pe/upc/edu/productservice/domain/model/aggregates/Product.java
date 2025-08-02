/**
 * Product aggregate
 * @Summary
 * The product class is an aggregate root that represents a product in the system.
 */
package pe.upc.edu.productservice.domain.model.aggregates;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import pe.upc.edu.productservice.domain.model.commands.CreateProductCommand;
import pe.upc.edu.productservice.domain.model.valueobjects.ProductType;

@Getter
@Setter
@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private ProductType productType;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 10)
    private Double balance;

    public Product() {
        this.productType = ProductType.CREDIT_CARD;
        this.name = "";
        this.balance = 0.0;
    }

    public Product(ProductType productType, String name, Double balance) {
        this.productType = productType;
        this.name = name;
        this.balance = balance;
    }

    public Product(CreateProductCommand command) {
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