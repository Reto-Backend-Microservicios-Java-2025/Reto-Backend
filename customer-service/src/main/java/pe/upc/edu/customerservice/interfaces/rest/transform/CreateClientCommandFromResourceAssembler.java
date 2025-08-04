package pe.upc.edu.customerservice.interfaces.rest.transform;

import pe.upc.edu.customerservice.domain.model.commands.CreateClientCommand;
import pe.upc.edu.customerservice.interfaces.rest.resources.CreateClientResource;

public class CreateClientCommandFromResourceAssembler {
    public static CreateClientCommand toCommandFromResource(CreateClientResource resource) {
        return new CreateClientCommand(
            resource.full_name(),
            resource.full_last_name(),
            resource.type_document(),
            resource.number_document(),
            resource.uniqueCode()
        );
    }
}
