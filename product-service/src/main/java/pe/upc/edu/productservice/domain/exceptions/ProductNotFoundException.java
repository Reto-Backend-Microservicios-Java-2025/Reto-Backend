package pe.upc.edu.productservice.domain.exceptions;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(Long aLong) {
        super("Product with id " + aLong + " not found");
    }
}
