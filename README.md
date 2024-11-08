# ATM CLI Application

A command-line interface application that simulates ATM operations like checking balance, withdrawing money, transfering, and making deposits.

## Prerequisites

Before running this application, make sure you have Docker installed on your system:

- [Docker Desktop for Windows](https://docs.docker.com/desktop/windows/install/)
- [Docker Desktop for Mac](https://docs.docker.com/desktop/mac/install/)
- [Docker Engine for Linux](https://docs.docker.com/engine/install/)

## Installation

1. Clone the repository

```bash
git clone https://github.com/ad17-2/atm-cli
cd atm-cli
```

2. If you already have the repository

```bash
cd atm-cli
```

## Usage

For easier to develop, build, test, and enter the CLI, the author have made a Makefile to make it easier for other people to run the system.

1. Test the application:

```bash
make test
```

2. Build & Run the application using Docker:

```bash
make run
```

3. Enter the CLI using Docker:

```bash
make cli
```

## Available Commands

- `register <username> <password>` - Create a new account
- `login <username> <password>` - Log into your account
- `logout` - Log out of current account
- `deposit <amount>` - Deposit money
- `withdraw <amount>` - Withdraw money
- `transfer <username> <amount>` - Transfer money to another user
- `balance` - Check your balance
- `help` - Show this help message
- `exit` - Exit the application

## Example Usage

```bash
$ make cli
docker-compose exec app java -jar app.jar
Welcome to ATM CLI!
Type 'help' for available commands, 'exit' to quit.
>
register omron KentangAjaib123
Registration successful with username: omron
>
login omron KentangAjaib123
Hello, omron
>
balance
Balance: $0.0000
>
deposit 1000
Deposit successful. New balance: $1000.0000
>
balance
Balance: $1000.0000
>
withdraw 500
Withdraw successful. New balance: $500.0000
>
balance
Balance: $500.0000
>
logout
Goodbye, omron
>
exit
Shutting down...
```

## Core Features

- Register
- Login
- Balance
- Deposit
- Withdraw
- Transfer
- Session Management
- Transaction Logging
- Logout

## Features Consideration

### Register

Enables new users to create an account, essential for logging in and performing transactions.

#### Security Considerations

- Prevents duplicate registrations
- Username:
  - Must be non-empty, 3-30 characters long, and alphanumeric
- Password:
  - Must be at least 8 characters, with one uppercase letter, one lowercase letter, and one number
  - Stored securely with Bcrypt

### Login

Allows existing users to log in to access ATM functionality.

#### Security Considerations

- Ensures only registered users can log in
- Enforces single-user login at any time
- Verifies credentials with strong password validation

### Balance Inquiry

Displays the user current ATM balance.

#### Security Considerations

##### Security

- Only logged-in users can check their balance

##### User Experience

Provides an easy way for users to check their balance, mimicking real-world ATM functionality where balance inquiries are frequent.

### Deposit

Allows users to deposit money into their ATM balance.

#### Security Considerations

- Restricted to logged-in users
- Accepts only valid numbers and amounts greater than 1

### Withdraw

Enables users to withdraw funds from their balance.

#### Security Considerations

- Restricted to logged-in users
- Accepts only valid numbers and amounts greater than 1
- Enforces an “insufficient balance” check to prevent overdraws

### Transfer

Made to cater the needs of user to Transfer to another user

#### Security Considerations

- Restricted to logged-in users
- Accepts only valid numbers and amounts greater than 1
- Verifies recipient existence and prevents transfers to self (use deposit instead)
- Enforces “insufficient balance” check to avoid over-transfers

#### Additional Transfer Design Considerations:

#### User Psychology

- Preventing anxiety from user side on "overspending" or "falling into debt" when they overtransfer

#### Technical

- Simpler logic : balance check is a yes / no question.
- There's no need for complex debt calculation system, i.e interest, grace periods, late fee, etc
- Easier to rollbacks
- There's no need for the system to maintain debt relationships between users, i believe its more appropriate for the bank it self to maintain the debt user <-> bank, not user <-> user

#### Risk Management

- Zero risk of user defaulting their balance
- Bank needs to asses their credit first via credit scoring before lending user money

#### Clarity

- Users always know exactly how much they can transfer

### Session Management

Ensures only one user is logged in at a time, with a timeout for inactive sessions (currently set to 1 minute) to prevent unauthorized access.

### Transaction Logging

Records transactions to support financial tracking and facilitate dispute resolution.
