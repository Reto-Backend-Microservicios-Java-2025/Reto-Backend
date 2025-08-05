package pe.upc.edu.customerservice.interfaces.rest.transform;

import pe.upc.edu.customerservice.domain.model.aggregates.Client;
import pe.upc.edu.customerservice.interfaces.rest.resources.ClientResource;
import pe.upc.edu.customerservice.infrastructure.EncryptionUtil;

public class ClientResourceFromEntityAssembler {

    public static ClientResource toResourceFromEntity(Client entity) {
        try {
            // Encriptar el uniqueCode (suponiendo que es Long)
            String encryptedCode = EncryptionUtil.encrypt(String.valueOf(entity.getUniqueCode()));

            return new ClientResource(
                    entity.getId(),
                    entity.getFullName(),
                    entity.getFullLastName(),
                    entity.getTypedocument(),
                    entity.getDocumentNumber(),
                    encryptedCode
            );

        } catch (Exception e) {
            throw new RuntimeException("Error al encriptar uniqueCode", e);
        }
    }

}