Feature: Add New User

As a Lego(R) Part Warehouse Employee
I would like become a user of the Lego(R) Part Warehouse Manager System
So that I can perform Lego(R) Part Warehouse Manager transactions related to my job function

Scenario Outline: Different types of users (Normal Flow)

Given employee <emp_name> with employee id <emp_id> is a employee type <emp_type> in good standing
When employee <emp_name> requests user access to the Lego(R) Part Warehouse Manager System
Then a new <user_name> and initial <password> are generated

| emp_name       | emp_id | emp_type            | user_name | password |
| Archie Andrews |AA001   |Operator             |Andrews_A  |aa001     |
| Betty Cooper   |CB002   |Inventory Manager    |Cooper_B   |cb002     |
| Jughead Jones  |JJ003   |System Administrator |Jones_J    |jj003     |
| Veronica Lodge |LV004   |Auditor              |Lodge_V    |lv004     |
| Reggie Mantle  |MR005   |Warehouse picker     |Mantle_R   |mr005     |l| Ethel Mudd     |ME006   |Consumer             |Mudd_E     |me006     |

Scenario Outline: Non Employee Attempts to become user (Error Flow)

Given Fred Smith uses id INVALID_ID to request Operator user access
When employee Fred Smith requests user access to the Lego(R) Part Warehouse Manager System
Then an "Unauthorized request" message is issued
And a record of the attempt is send to the System Administrator

Scenario Outline: Existing user attempts to become a user (Error Flow)

Given Bill Jones is user of the Lego(R) Part Warehouse System
When Bill Jones requests user access to the Lego(R) Part Warehouse System
Then an "Already registered" message is issued
