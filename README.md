# AccountService
REST Service with InMemory storage

Usage and API:
Use a Rest client such as Postman chrome 
1) Create an account to be source of the money transfer:
````
Method: Post
Uri:    http://127.0.0.1:8081/accounts
Header: Content-Type: application/json
````
Body:
```json
{
  "name":"Joey",
  "balance":200
}
````
Response: Note the account `number` in the successful response with Http Status Code = 201 Created:
```json
{
  "number": "1000",
  "name": "Joey",
  "balance": 200
}
```

2) Create another account to be destination of the money transfer: 
Body:
```json
{
  "name":"JoeJoeJr",
  "balance":0
}
````

3) Perform transfer from source account number as path param with value `1000` representing `Joey`:
````
Method: Post
Uri:    http://127.0.0.1:8081/accounts/1000/transfer
Header: Content-Type: application/json
````
Body:
```json
{
  "destAccNum":"1001",
  "transferAmount":99
}
````
Response: Successful transfer will have Http Status Code = 200 OK
```json
{
  "sourceAccount": {
    "number": "1000",
    "name": "Joey",
    "balance": 101
  },
  "destAccount": {
    "number": "1001",
    "name": "JoeJoeJr",
    "balance": 99
  },
  "transferAmount": 99
}
```
Errors: 
- Insufficient funds in source account will have Http Status Code = 400 Bad Request
```json
{
  "sourceAccNum": "1000",
  "destAccNum": "1001",
  "transferAmount": 99,
  "description": "Not enough funds available in account: 1000 "
}
```
- Account Not Found for source account path param or destination `destAccNum` in json body will have Http Status Code = 404 Not Found
```json
{
  "accountNumber": "1000000",
  "description": "Account Number doesn't exist: 1000000"
}
```

TODO:
- Unit tests

- Add comments to code where appropriate

- Add readme API docs
