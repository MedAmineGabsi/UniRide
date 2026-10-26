package ca.mcgill.ecse428.UniRide_Backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "person_role_assignment", uniqueConstraints = @UniqueConstraint(columnNames = { "person_id",
        "role_id" }))
public class PersonRoleAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private PersonRole role;

    protected PersonRoleAssignment() {
    }

    public PersonRoleAssignment(Person person, PersonRole role) {
        this.person = person;
        this.role = role;
    }

    public Integer getId() {
        return id;
    }

    public Person getPerson() {
        return person;
    }

    public PersonRole getRole() {
        return role;
    }
}
