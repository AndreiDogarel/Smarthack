package com.example.dodo.entities;

import lombok.*;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
    private String username;
    private String role;

    public UserDto(User user) {
        this.username = user.getUsername();
        this.role = user.getRole().getName().name();
    }
}
//public interface PersonRepository extends JpaRepository<Person, Long> {
//    @Query("select p from Person p where p.emailAddress = ?1")
//    Person findByEmailAddress(String emailAddress);
//}
