package ca.mcgill.ecse428.UniRide_Backend.model;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

@Entity
@Table(name = "person_role")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "role_type", discriminatorType = DiscriminatorType.STRING, length = 20)
public abstract class PersonRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    public Integer getId() {
        return id;
    }
}
