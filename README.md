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
