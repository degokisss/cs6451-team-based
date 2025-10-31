## Model - Login/Logout
### Login
1. username-password login
2. google login: need to use Google-SDK
3. Entity: **Customer**
Do you think we need to support **Authentication Token Mechanism**?
### Registration 
Using username-password to register, make sure hash the password before saving it to database.
### Login out

## Model - Booking
1. Make sure the lock mechanism is working properly.
2. Entity: **Order** 
### Searching
### Booking
### Cancel

## Model - Payment
1. Make sure update the status of Order after payment finish

## Model - Management
1. Customer Management: delete User...
2. Hotel Management: add hotel
3. Room Management: add room
4. price management: change price


## Model - Check-in/Check-out
1. Make sure using Notification Service and generate a QR-Code for check-in
2. update the order status after check-in and check-out

## Base Service

### Notification Service
1. Email Notification
2. SMS Notification

### Price Service
1. Dynamic Price

### Log Service
1. Log the user's action to a file, in order to recover the data.