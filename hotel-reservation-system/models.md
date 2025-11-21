## Module - Login/Logout
1. Login
- username-password login
- google login: need to use Google-SDK
- Entity: **Customer**
Do you think we need to support **Authentication Token Mechanism**
2. Registration 
Using username-password to register, make sure hash the password before saving it to database.
3. Login out

## Module - Booking
**State Pattern**
- Make sure the lock mechanism is working properly.
- Entity: **Order** 
1. Searching
2. Booking
3. <span style="color:red">Cancel</span>

## Module - Payment
**Factory Pattern**
- Make sure update the status of Order after payment finish
- <span style="color:red">Finished the business logic</span>

## Module - Management
1. ~~<span style="color:red">Customer Management: delete User...</span>~~
2. Hotel Management: add hotel
3. Room Management: add room
4. <span style="color:red">price management: change price</span>

## <span style="color:red">Module - Check-in/Check-out</span>
1. Make sure using Notification Service and generate a QR-Code for check-in
2. update the order status after check-in and check-out

## Base Service

### Notification Service
**Factory Pattern**
1. Email Notification
2. SMS Notification

### Price Service 
**Strategy Pattern**
**Observer Pattern**
**Decorator Pattern?**

Audit logging (Spring AOP)