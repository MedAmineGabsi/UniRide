Feature: Add New User

As a McGill Student
I would like become a user of the UniRide ride sharing app
So that I can participate in ride shares with other UniRide users

Scenario Outline: Different types of users (Normal Flow)

Given a McGill student with first name <first_name>, last name <last_name>, student ID <student_id>, email <email> and phone number <phone_number> is in good standing
When the student requests to register as a UniRide user with password <password> and role <role> and license number <license_number>
Then a new UniRide user is created with student ID <student_id> and email <email>
And the user is assigned the role <role>

| first_name | last_name | student_id | email                         | phone_number | password    | role                 | license_number |
| Alice      | Tremblay  | 260100001  | alice.tremblay@mail.mcgill.ca | 514-555-0101 | Str0ngPass! | Passenger            |                |
| Bob        | Chen      | 260100002  | bob.chen@mail.mcgill.ca       | 514-555-0102 | Str0ngPass! | Driver               | D1234-567890   |
| Carla      | Singh     | 260100003  | carla.singh@mcgill.ca         | 514-555-0103 | Str0ngPass! | Passenger and Driver | S9876-543210   |

Scenario Outline: Email is not case sensitive (Normal Flow)

Given no UniRide user exists with email <stored_email>
When student Dana Roy registers with email <entered_email>
Then a new UniRide user is created with email <stored_email>

| stored_email                | entered_email               |
| dana.roy@mail.mcgill.ca     | Dana.Roy@Mail.McGill.ca     |

Scenario Outline: Non McGill Student Attempts to become user (Error Flow)

Given Fred Smith uses email <email> to request UniRide user access
When Fred Smith requests to register as a UniRide user
Then an "Unauthorized request" message is issued
And no UniRide user is created with email <email>

| email                        |
| fred.smith@gmail.com         |
| fred.smith@concordia.ca      |
| fred.smith@mcgill.ca.fake.com|

Scenario Outline: Existing user attempts to become a user (Error Flow)

Given Bill Jones is already a registered user of UniRide with email <email>
When Bill Jones requests to register as a UniRide user with email <entered_email>
Then an "Already registered" message is issued
And no additional UniRide user is created

| email                      | entered_email              |
| bill.jones@mail.mcgill.ca  | bill.jones@mail.mcgill.ca  |
| bill.jones@mail.mcgill.ca  | Bill.Jones@Mail.McGill.ca  |

Scenario Outline: Student registers with a duplicate student ID (Error Flow)

Given a UniRide user exists with student ID <student_id>
When student Gina Park with student ID <student_id> requests to register as a UniRide user
Then an "Already registered" message is issued
And no additional UniRide user is created

| student_id |
| 260100001  |

Scenario Outline: Driver registers with a duplicate license number (Error Flow)

Given a UniRide driver exists with license number <license_number>
When student Omar Haddad requests to register as a UniRide user with role Driver and license number <license_number>
Then an "License number already in use" message is issued
And no additional UniRide user is created

| license_number |
| D1234-567890   |

Scenario Outline: Registration with an invalid email format (Error Flow)

Given student Hana Ito is a McGill student in good standing
When Hana Ito requests to register as a UniRide user with email <email>
Then an "Invalid email format" message is issued
And no UniRide user is created

| email                  |
| hana.ito               |
| hana.ito@              |
| @mail.mcgill.ca        |
| hana ito@mcgill.ca     |

Scenario Outline: Registration with a missing required field (Error Flow)

Given student Ivan Petrov is a McGill student in good standing
When Ivan Petrov requests to register as a UniRide user with <field> left blank
Then a "<field> is required" message is issued
And no UniRide user is created

| field          |
| first name     |
| last name      |
| email          |
| password       |
| student ID     |
| phone number   |

Scenario: Driver registration with a missing license number (Error Flow)

Given student Ivan Petrov is a McGill student in good standing
When Ivan Petrov requests to register as a UniRide user with role Driver and license number left blank
Then a "license number is required" message is issued
And no UniRide user is created

Scenario Outline: Registration with a weak password (Error Flow)

Given student Jade Wong is a McGill student in good standing
When Jade Wong requests to register as a UniRide user with password <password>
Then a "Password does not meet requirements" message is issued
And no UniRide user is created

| password     |
| abc          |
| password     |
| 12345678     |

Scenario Outline: Password confirmation does not match (Error Flow)

Given student Kai Lopez is a McGill student in good standing
When Kai Lopez requests to register with password <password> and confirmation <confirmation>
Then a "Passwords do not match" message is issued
And no UniRide user is created

| password      | confirmation  |
| Str0ngPass!   | Str0ngPass?   |
