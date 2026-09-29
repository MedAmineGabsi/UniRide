Feature: Add New User

As a McGill Student
I would like become a user of the UniRide ride sharing app
So that I can participate in ride shares with other UniRide users

Scenario Outline: Different types of users (Normal Flow)

Given student <student_name> with student id <student_id> and email <email> is a McGill student in good standing
When student <student_name> requests to register as a UniRide user with role <role>
Then a new UniRide account is created for <email> with role <role>
And a verification email is sent to <email>

| student_name  | student_id | email                      | role             |
| Alice Tremblay| 260100001  | alice.tremblay@mail.mcgill.ca | Rider         |
| Bob Chen      | 260100002  | bob.chen@mail.mcgill.ca    | Driver           |
| Carla Singh   | 260100003  | carla.singh@mcgill.ca      | Rider and Driver |

Scenario Outline: Email is not case sensitive (Normal Flow)

Given no UniRide account exists for email <stored_email>
When student Dana Roy registers with email <entered_email>
Then a new UniRide account is created for <stored_email>

| stored_email                | entered_email               |
| dana.roy@mail.mcgill.ca     | Dana.Roy@Mail.McGill.ca     |

Scenario Outline: Non McGill Student Attempts to become user (Error Flow)

Given Fred Smith uses email <email> to request UniRide user access
When Fred Smith requests to register as a UniRide user
Then an "Unauthorized request" message is issued
And no UniRide account is created for <email>

| email                        |
| fred.smith@gmail.com         |
| fred.smith@concordia.ca      |
| fred.smith@mcgill.ca.fake.com|

Scenario Outline: Existing user attempts to become a user (Error Flow)

Given Bill Jones is already a registered user of UniRide with email <email>
When Bill Jones requests to register as a UniRide user with email <entered_email>
Then an "Already registered" message is issued
And no additional UniRide account is created

| email                      | entered_email              |
| bill.jones@mail.mcgill.ca  | bill.jones@mail.mcgill.ca  |
| bill.jones@mail.mcgill.ca  | Bill.Jones@Mail.McGill.ca  |

Scenario Outline: Student registers with a duplicate student id (Error Flow)

Given a UniRide user exists with student id <student_id>
When student Gina Park with student id <student_id> requests to register as a UniRide user
Then an "Already registered" message is issued
And no additional UniRide account is created

| student_id |
| 260100001  |

Scenario Outline: Registration with an invalid email format (Error Flow)

Given student Hana Ito is a McGill student in good standing
When Hana Ito requests to register as a UniRide user with email <email>
Then an "Invalid email format" message is issued
And no UniRide account is created

| email                  |
| hana.ito               |
| hana.ito@              |
| @mail.mcgill.ca        |
| hana ito@mcgill.ca     |

Scenario Outline: Registration with a missing required field (Error Flow)

Given student Ivan Petrov is a McGill student in good standing
When Ivan Petrov requests to register as a UniRide user with <field> left blank
Then a "<field> is required" message is issued
And no UniRide account is created

| field      |
| name       |
| email      |
| password   |
| student id |

Scenario Outline: Registration with a weak password (Error Flow)

Given student Jade Wong is a McGill student in good standing
When Jade Wong requests to register as a UniRide user with password <password>
Then a "Password does not meet requirements" message is issued
And no UniRide account is created

| password     |
| abc          |
| password     |
| 12345678     |

Scenario Outline: Password confirmation does not match (Error Flow)

Given student Kai Lopez is a McGill student in good standing
When Kai Lopez requests to register with password <password> and confirmation <confirmation>
Then a "Passwords do not match" message is issued
And no UniRide account is created

| password      | confirmation  |
| Str0ngPass!   | Str0ngPass?   |

Scenario Outline: Verification link is not used (Error Flow)

Given student Lena Fox has registered with email <email> and the account is pending verification
When the verification link has <link_status>
Then the account for <email> is not activated
And a "Verification link expired" message is issued if the link is used

| email                    | link_status                  |
| lena.fox@mail.mcgill.ca  | expired after 24 hours       |
