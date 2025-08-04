package pe.upc.edu.customerservice.domain.model.aggregates;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import jakarta.validation.constraints.NotBlank;
import org.jetbrains.annotations.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import pe.upc.edu.customerservice.domain.model.commands.CreateClientCommand;
import pe.upc.edu.customerservice.domain.model.valueobjects.TypeDocument;

@Getter
@Setter
@Table("clients")
public class Client {

    @Id
    private Long id;

    @NotNull
    @NotBlank
    @Size(max = 70)
    @Column("full_name")
    private String fullName;

    @NotNull
    @NotBlank
    @Size(max = 70)
    @Column("full_last_name")
    private String fullLastName;

    @Column("typedocument")
    private TypeDocument typedocument;

    @NotNull
    @NotBlank
    @Size(min = 6, max = 20)
    @Column("document_number")
    private String documentNumber;

    @NotNull
    @Column("unique_code")
    private Long uniqueCode;

    public Client() {
        this.typedocument = TypeDocument.DNI;
    }

    public Client(String fullName, String fullLastName, TypeDocument typedocument, String documentNumber) {
        this.fullName = fullName;
        this.fullLastName = fullLastName;
        this.typedocument = typedocument;
        this.documentNumber = documentNumber;
    }

    public Client(CreateClientCommand command) {
        this.fullName = command.full_name();
        this.fullLastName = command.full_last_name();
        this.typedocument = command.type_document();
        this.documentNumber = command.number_document();
        this.uniqueCode = command.uniqueCode();
    }

    public Client updateInformation(String fullName, String fullLastName, TypeDocument typedocument, String documentNumber, Long uniqueCode) {
        this.fullName = fullName;
        this.fullLastName = fullLastName;
        this.typedocument = typedocument;
        this.documentNumber = documentNumber;
        this.uniqueCode = uniqueCode;
        return this;
    }
}