package pe.upc.edu.productservice.domain.model.valueobjects;

/**
 * Enum representing supported financial product types.
 */
public enum ProductType {
    // Cuenta de ahorros: permite guardar dinero y ganar intereses.
    SAVINGS_ACCOUNT,
    // Cuenta corriente: usada para realizar pagos y transferencias frecuentes.
    CHECKING_ACCOUNT,
    // Tarjeta de crédito: permite hacer compras a crédito y pagar después.
    CREDIT_CARD,
    // Tarjeta de débito: permite realizar compras y retiros usando fondos disponibles.
    DEBIT_CARD,
    // Préstamo: dinero prestado que debe devolverse con intereses.
    LOAN,
    // Cuenta de inversión: permite invertir en acciones, bonos, fondos, etc.
    INVESTMENT_ACCOUNT,
    // Póliza de seguro: contrato para protegerse contra riesgos (vida, salud, autos, etc.).
    INSURANCE_POLICY
}