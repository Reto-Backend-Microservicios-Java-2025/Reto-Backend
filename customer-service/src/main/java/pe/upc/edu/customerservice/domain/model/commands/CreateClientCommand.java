package pe.upc.edu.customerservice.domain.model.commands;

import pe.upc.edu.customerservice.domain.model.valueobjects.TypeDocument;

public record CreateClientCommand(
    String full_name,
    String full_last_name,
    TypeDocument type_document,
    String number_document,
    Long uniqueCode

) {

}
